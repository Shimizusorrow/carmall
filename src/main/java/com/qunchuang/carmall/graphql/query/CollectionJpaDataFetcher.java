package com.qunchuang.carmall.graphql.query;

import com.qunchuang.carmall.graphql.IGraphQlTypeMapper;
import com.qunchuang.carmall.graphql.query.enums.QueryFilterOperator;
import com.qunchuang.carmall.graphql.query.enums.QueryForWhatEnum;
import com.qunchuang.carmall.graphql.GraphQLSchemaBuilder;
import graphql.language.*;
import com.qunchuang.carmall.graphql.query.enums.QueryFilterCombinator;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.util.StringUtils;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.*;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class CollectionJpaDataFetcher extends JpaDataFetcher {

    public static final String ENTITY_PROP_FOR_DISABLED = "disabled";

    public CollectionJpaDataFetcher(EntityManager entityManager, EntityType<?> entityType, IGraphQlTypeMapper graphQlTypeMapper) {
        super(entityManager, entityType, graphQlTypeMapper);
    }

    @Override
    public Object getResult(DataFetchingEnvironment environment, QueryFilter queryFilter) {
        Field field = environment.getFields().iterator().next();
        Map<String, Object> result = new LinkedHashMap<>();
        Paginator pageInformation = extractPageInformation(environment, field);
        // See which fields we're requesting
        Optional<Field> totalPagesSelection = getSelectionField(field, "totalPages");
        Optional<Field> totalElementsSelection = getSelectionField(field, "totalElements");
        Optional<Field> contentSelection = getSelectionField(field, "content");

        Long totalElements = 0L;
        if (totalElementsSelection.isPresent() || totalPagesSelection.isPresent()) {
            totalElements = contentSelection
                    .map(contentField -> getCountQuery(environment, contentField, queryFilter).getSingleResult())
                    // if no "content" was selected an empty Field can be used
                    .orElseGet(() -> getCountQuery(environment, new Field(""), queryFilter).getSingleResult());

            result.put("totalElements", totalElements);
            result.put("totalPages", ((Double) Math.ceil(totalElements / (double) pageInformation.getSize())).longValue());
        }

        if (contentSelection.isPresent()) {
            List queryResult = null;

            //if(totalElements==0){
            //什么都不做
            //}else
            //如果查询比较复杂，含有collection，需要分步查询的话。
            if (isIncludeCollection(entityType, contentSelection.get().getSelectionSet())) {
                //1.找出ids
                TypedQuery typedQuery = getQueryForEntity(environment, queryFilter, contentSelection.get(), QueryForWhatEnum.JUSTFORIDSINTHEPAGE, null);
                List<String> ids = new PaginatorFactory(this.entityManager, this.entityType).getPaginator(typedQuery, pageInformation);
                //2.准备nativesql，设置参数，设定返回值，执行。
                if (ids != null && ids.size() > 0) {
                    QueryFilter qf = new QueryFilter("id", QueryFilterOperator.IN, StringUtils.collectionToDelimitedString(ids, ",", "'", "'"), QueryFilterCombinator.AND, null);
                    //3.去掉任何查询条件，只使用 ids in，并去掉paginator, 查询结果
                    queryResult = getQueryForEntity(environment, qf, contentSelection.get(), QueryForWhatEnum.NORMAL, null).getResultList();
                }
            } else {
                queryResult = getQueryForEntity(environment, queryFilter, contentSelection.get(), QueryForWhatEnum.NORMAL, pageInformation).getResultList();
            }
            //给定ids并执行各查询，
            // 返回结果并组合到queryResult中
            result.put("content", queryResult);
        }

        return result;
    }

    /**
     * 本次查询中有没有包含collection元素
     */
    private boolean isIncludeCollection(ManagedType entityType, SelectionSet fields) {
        if (fields == null || entityType == null) {
            return false;
        }
        for (Selection select : fields.getSelections()) {
            Optional<Attribute> selectedFieldAttributeOptional = entityType.getAttributes()
                    .stream()
                    .filter(attr -> ((Attribute) attr).getName().equals(((Field) select).getName()))
                    .findFirst();
            if (selectedFieldAttributeOptional.isPresent()) {
                Attribute selectedFieldAttribute = selectedFieldAttributeOptional.get();
                if (selectedFieldAttribute == null) {//有时候的确没有，如__typename
                    return false;
                } else if (selectedFieldAttribute instanceof PluralAttribute &&
                        ((PluralAttribute) selectedFieldAttribute).getPersistentAttributeType() == Attribute.PersistentAttributeType.ONE_TO_MANY) {
                    return true;
                } else if (selectedFieldAttribute instanceof SingularAttribute
                        && ((SingularAttribute) selectedFieldAttribute).getPersistentAttributeType() == Attribute.PersistentAttributeType.MANY_TO_ONE) {
                    return isIncludeCollection(selectedFieldAttribute.getDeclaringType(), ((Field) select).getSelectionSet());
                }
            }
        }
        return false;
    }

    /**
     * 用来方便继承的
     *
     * @param qfilter          - 过滤条件
     * @param field            -字段信息
     * @param queryForWhatEnum - 是否仅仅查询数量，
     * @return 如果仅仅查询数量则返回TypedQuery<Long>、如果查询的是Entity，则返回TypedQuery<EntityType>
     */
    protected TypedQuery getQueryForEntity(DataFetchingEnvironment environment, QueryFilter qfilter, Field field, QueryForWhatEnum queryForWhatEnum, Paginator paginator) {

        return super.getQuery(environment, field, qfilter, queryForWhatEnum, paginator);
    }

    //用来方便继承的。
    protected Object getForEntity(DataFetchingEnvironment environment, QueryFilter qfilter) {
        return super.getResult(environment, qfilter);
    }

    private TypedQuery<Long> getCountQuery(DataFetchingEnvironment environment, Field field, QueryFilter qfilter) {
        return getQueryForEntity(environment, qfilter, field, QueryForWhatEnum.JUSTFORCOUNTBYDISTINCTID, null);
    }

    private Optional<Field> getSelectionField(Field field, String fieldName) {
        return field.getSelectionSet().getSelections().stream().filter(it -> it instanceof Field).map(it -> (Field) it).filter(it -> fieldName.equals(it.getName())).findFirst();
    }

    private Paginator extractPageInformation(DataFetchingEnvironment environment, Field field) {
        Optional<Argument> paginationRequest = field.getArguments().stream().filter(it -> GraphQLSchemaBuilder.PAGINATION_REQUEST_PARAM_NAME.equals(it.getName())).findFirst();
        if (paginationRequest.isPresent()) {
            Value v = paginationRequest.get().getValue();
            return (Paginator) this.convertValue(environment, this.graphQlTypeMapper.getGraphQLInputType(Paginator.class), v, 0);
        }
        return new Paginator(1, Integer.MAX_VALUE);
    }

    @Override
    protected QueryFilter extractQueryFilter(DataFetchingEnvironment environment, Field field) {
        Optional<Argument> qfilterRequest = field.getArguments().stream().filter(it -> GraphQLSchemaBuilder.QFILTER_REQUEST_PARAM_NAME.equals(it.getName())).findFirst();
        QueryFilter qfilter = null;
        if (qfilterRequest.isPresent()) {
            Object qfilterValues = qfilterRequest.get().getValue();
            qfilter = (QueryFilter) this.convertValue(environment, this.graphQlTypeMapper.getGraphQLInputType(QueryFilter.class), qfilterValues, 0);
        }

        //如果是也包含禁用条目
        if (qfilter != null && qfilter.isDisabledEntityAllowed()) {
            qfilter = qfilter.getNext();
            //如果是"只"包含禁用条目
        } else if (qfilter != null && qfilter.isOnlyDisabledEntityAllowed()) {
        } else {//否则是不包含禁用条目
            qfilter = new QueryFilter(ENTITY_PROP_FOR_DISABLED, QueryFilterOperator.EQUEAL, "false", QueryFilterCombinator.AND, qfilter);
        }
        return qfilter;
    }

}

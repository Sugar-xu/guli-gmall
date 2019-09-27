package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.util.RedisUtil;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.apache.lucene.queryparser.xml.builders.FilteredQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import javax.swing.text.Highlighter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private JestClient jestClient;

    @Autowired
    private RedisUtil redisUtil;

    public static final String ES_INDEX = "gmall";
    public static final String ES_TYPE = "SkuInfo";

    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE).id(skuLsInfo.getId()).build();

        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {

        String query = makeQueryStringForSearch(skuLsParams);
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        SearchResult searchResult = null;
        try {
            searchResult = jestClient.execute(search);
        } catch (IOException e) {

        }
        SkuLsResult skuLsResult = makeResultForSearch(skuLsParams, searchResult);
        return skuLsResult;
    }

    @Override
    public void incrHotScore(String skuId) {
        //获取redis
        Jedis jedis = redisUtil.getJedis();
        String hotKey = "hotScore";
        //设置每次加1
        Double hotScore = jedis.zincrby(hotKey, 1, "skuId:" + skuId);

        jedis.close();
        //设置更新es
        if (hotScore % 10 == 0) {
            updateHostScore(skuId, Math.round(hotScore));
        }
    }

    private void updateHostScore(String skuId, long hotScore) {
        String updateJson = "{\n" +
                "  \"doc\": {\n" +
                "    \"hotScore\":" + hotScore + "\n" +
                "  }\n" +
                "}";
        Update update = new Update.Builder(updateJson).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
        try {
            jestClient.execute(update);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //设置返回结果
    private SkuLsResult makeResultForSearch(SkuLsParams skuLsParams, SearchResult searchResult) {
        SkuLsResult skuLsResult = new SkuLsResult();
        //获取每一个属性的值
//        List<SkuLsInfo> skuLsInfoList;
        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source;
            //获取高亮skuName;
            Map<String, List<String>> highlight = hit.highlight;
            if (highlight != null && highlight.size() > 0) {
                List<String> skuNameList = highlight.get("skuName");
                String skuNameHI = skuNameList.get(0);
                skuLsInfo.setSkuName(skuNameHI);
            }
            skuLsInfoList.add(skuLsInfo);
        }
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
//        long total;
        skuLsResult.setTotal(searchResult.getTotal());
//        long totalPages;
        long totalPages = (searchResult.getTotal() + skuLsParams.getPageSize() - 1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPages);
//
//        List<String> attrValueIdList;
        List<String> attrValueIdList = new ArrayList<>();
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation group_attr = aggregations.getTermsAggregation("group_attr");
        if (group_attr != null) {
            List<TermsAggregation.Entry> buckets = group_attr.getBuckets();
            for (TermsAggregation.Entry bucket : buckets) {
                String valueId = bucket.getKey();
                attrValueIdList.add(valueId);
            }
            skuLsResult.setAttrValueIdList(attrValueIdList);
        }

        return skuLsResult;
    }

    //生成动态dsl语句
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //创建build
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //创建bool
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //判断skuName检索
        if (skuLsParams.getKeyword() != null) {
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName", skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);
            //如果存在关键字检错，需要设置高亮显示
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.field("skuName");
            highlightBuilder.preTags("<span style='color:red'>");
            highlightBuilder.postTags("</span>");
            //将设置的高亮显示放入查询器
            searchSourceBuilder.highlight(highlightBuilder);
        }
        //判断三级分类
        if (skuLsParams.getCatalog3Id() != null) {
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //判断平台属性值
        if (skuLsParams.getValueId() != null && skuLsParams.getValueId().length > 0) {
            for (String valueId : skuLsParams.getValueId()) {
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId", valueId);
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        searchSourceBuilder.query(boolQueryBuilder);
        //设置分页
        int from = (skuLsParams.getPageNo() - 1) * skuLsParams.getPageSize();
        searchSourceBuilder.from(from);
        searchSourceBuilder.size(skuLsParams.getPageSize());
        //设置排序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        //设置聚合
        TermsBuilder group_attr = AggregationBuilders.terms("group_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(group_attr);

        String query = searchSourceBuilder.toString();
        System.out.println(query);

        return query;
    }
}

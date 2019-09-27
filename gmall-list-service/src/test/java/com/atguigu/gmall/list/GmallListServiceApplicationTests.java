package com.atguigu.gmall.list;

import io.searchbox.client.JestClient;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RunWith(SpringRunner.class)
@SpringBootTest
public class GmallListServiceApplicationTests {

	@Autowired
	private JestClient jestClient;

	@Test
	public void testES() {
		String query="{\n" +
				"  \"query\": {\n" +
				"    \"term\": {\n" +
				"      \"actorList.name\": \"张译\"\n" +
				"    }\n" +
				"  }\n" +
				"}";
		Search search = new Search.Builder(query).addIndex("movie_chn").addType("movie").build();

		try {
			SearchResult searchResult = jestClient.execute(search);
			List<SearchResult.Hit<Map, Void>> hits = searchResult.getHits(Map.class);
			for (SearchResult.Hit<Map, Void> hit : hits) {
				Map map = hit.source;
				System.out.println("name********"+map.get("name"));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}

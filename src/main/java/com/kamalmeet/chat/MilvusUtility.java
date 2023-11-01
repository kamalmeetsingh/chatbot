package com.kamalmeet.chat;

import io.milvus.client.MilvusClient;
import io.milvus.client.MilvusServiceClient;
import io.milvus.common.clientenum.ConsistencyLevelEnum;
import io.milvus.grpc.*;
import io.milvus.param.ConnectParam;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.*;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.param.index.CreateIndexParam;
import io.milvus.response.DescCollResponseWrapper;
import io.milvus.response.GetCollStatResponseWrapper;
import io.milvus.response.SearchResultsWrapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
@Component
public class MilvusUtility {

    //TODO Add host
    private String host="localhost";

    //TODO Add port

    private int port=19530;

    final Integer SEARCH_K = 3;                       // TopK
    final String SEARCH_PARAM = "{\"nprobe\":10}";
    
    private String collectionName= "myCollection";
    private String filename = "employees.csv";

    public MilvusServiceClient getClient(){

        MilvusServiceClient milvusClient = new MilvusServiceClient(
                ConnectParam.newBuilder()
                        .withHost(host)
                        .withPort(port)
                        .build()
        );

        return milvusClient;
    }

    public boolean createCollection(MilvusServiceClient milvusClient) {
        if(milvusClient == null){
            return false;
        }
        try {
            milvusClient.dropCollection(
                    DropCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build()
            );
            FieldType fieldType1 = FieldType.newBuilder()
                    .withName("employee_id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();
            FieldType fieldType2 = FieldType.newBuilder()
                    .withName("employee_data")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(2000)
                    .build();
            FieldType fieldType3 = FieldType.newBuilder()
                    .withName("employee_vector")
                    .withDataType(DataType.FloatVector)
                    .withDimension(1536)
                    .build();
            CreateCollectionParam createCollectionReq = CreateCollectionParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withDescription("Employee search")
                    .withShardsNum(2)
                    .addFieldType(fieldType1)
                    .addFieldType(fieldType2)
                    .addFieldType(fieldType3)
                    
                    .build();
            
            milvusClient.createCollection(createCollectionReq);
            IndexType INDEX_TYPE = IndexType.IVF_FLAT;   // IndexType
            milvusClient.createIndex(
                    CreateIndexParam.newBuilder()
                      .withCollectionName(collectionName)
                      .withFieldName("employee_vector")
                      .withIndexType(INDEX_TYPE)
                      .withMetricType(MetricType.L2)
                      .withSyncMode(Boolean.FALSE)
                      .build()
                  );
        }
        catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean insertData(MilvusServiceClient milvusClient){
        try {
            LLMUtility llmUtility = new LLMUtility();
            List<Long> app_id_array = new ArrayList<>();
            List<String> linearr = new ArrayList<>();
            List<List<Float>> search_vectors = new ArrayList<>();
            long i = 0;
            try {
                String line = "";
                BufferedReader br = new BufferedReader(new FileReader(filename));
                 
                while ((line = br.readLine()) != null)   //returns a Boolean value
                {
                    // ignore first row for headers
                    if(i==0) {
                        i++;
                        continue;
                    }
                    System.out.println(line);
                        
                    List<Float> vectors = llmUtility.getEmbeddings(line);
                    System.out.println("***"+vectors.size());
                    if (vectors.size() == 1536) {
                        app_id_array.add(i++);
                        linearr.add(line);
                        search_vectors.add(vectors);
                    } else {
                        System.out.println("not able to insert*******: "+line);
                    }

                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("employee_id", app_id_array));
            fields.add(new InsertParam.Field("employee_data", linearr));
            fields.add(new InsertParam.Field("employee_vector", search_vectors));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(collectionName)
                    .withFields(fields)
                    .build();
            milvusClient.insert(insertParam);
        } catch (Exception e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public String searchData(MilvusServiceClient milvusClient, String query){
        LLMUtility llmUtility = new LLMUtility();
        milvusClient.loadCollection(
                LoadCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );
        List<String> search_output_fields = Arrays.asList("employee_data");//lines
        List<Float> vectors = llmUtility.getEmbeddings(query);
        List<List<Float>> search_vectors = Arrays.asList(vectors);
        System.out.println("vector:"+vectors);
        SearchParam searchParam = SearchParam.newBuilder()
                .withCollectionName(collectionName)
                //.withIgnoreGrowing(true)
                //.withConsistencyLevel(ConsistencyLevelEnum.STRONG)
                .withMetricType(MetricType.L2)
                .withOutFields(search_output_fields)
                .withTopK(3)
                .withVectors(search_vectors)
                .withVectorFieldName("employee_vector")
                .build();
        R<SearchResults> respSearch = milvusClient.search(searchParam);
        SearchResultsWrapper wrapperSearch = new SearchResultsWrapper(respSearch.getData().getResults());
        var fieldData = wrapperSearch.getFieldData("employee_data", 0);
        //var fieldData = wrapperSearch.getFieldData("text", 0);
        var first = fieldData.get(0);
        return first.toString();
    }

    public void printData(MilvusServiceClient milvusClient){
        R<Boolean> respHasCollection = milvusClient.hasCollection(
                HasCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );
        if (respHasCollection.getData() == Boolean.TRUE) {
            System.out.println("Collection exists.");
        }

        R<DescribeCollectionResponse> respDescribeCollection = milvusClient.describeCollection(
                // Return the name and schema of the collection.
                DescribeCollectionParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );
        DescCollResponseWrapper wrapperDescribeCollection = new DescCollResponseWrapper(respDescribeCollection.getData());
        System.out.println(wrapperDescribeCollection);

        R<GetCollectionStatisticsResponse> respCollectionStatistics = milvusClient.getCollectionStatistics(
                // Return the statistics information of the collection.
                GetCollectionStatisticsParam.newBuilder()
                        .withCollectionName(collectionName)
                        .build()
        );
        GetCollStatResponseWrapper wrapperCollectionStatistics = new GetCollStatResponseWrapper(respCollectionStatistics.getData());
        System.out.println("Collection row count: " + wrapperCollectionStatistics.getRowCount());
        R<ShowCollectionsResponse> respShowCollections = milvusClient.showCollections(
                ShowCollectionsParam.newBuilder().build()
        );
        System.out.println(respShowCollections);
    }
    
    public void releaseCollection(MilvusClient milvusClient){
            milvusClient.releaseCollection(
                    ReleaseCollectionParam.newBuilder()
                            .withCollectionName(collectionName)
                            .build());
            milvusClient.close();
    }

}
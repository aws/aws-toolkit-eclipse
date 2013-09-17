package com.amazonaws.eclipse.explorer.dynamodb;

import java.util.LinkedList;
import java.util.List;

import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;


public class DynamoDBContentProvider extends AbstractContentProvider {

    private static DynamoDBContentProvider instance;
    
    public DynamoDBContentProvider() {
        instance = this;
    }
    
    public static DynamoDBContentProvider getInstance() {
        return instance;
    }

    public boolean hasChildren(Object element) {
        return element instanceof AWSResourcesRootElement || element instanceof DynamoDBRootNode;
    }

    @Override
    public Object[] loadChildren(Object parentElement) {
        if ( parentElement instanceof AWSResourcesRootElement) {
            return new Object[] { DynamoDBRootNode.NODE };
        }

        if ( parentElement == DynamoDBRootNode.NODE) {
            new DataLoaderThread(parentElement) {
                @Override
                public Object[] loadData() {
                    AmazonDynamoDB db = AwsToolkitCore.getClientFactory().getDynamoDBV2Client();
                    List<DynamoDBTableNode> nodes = new LinkedList<DynamoDBTableNode>();
                    ListTablesResult listTables = new ListTablesResult();
                    do {
                        listTables = db.listTables(new ListTablesRequest().withExclusiveStartTableName(listTables
                                .getLastEvaluatedTableName()));
                        for ( String tableName : listTables.getTableNames() ) {
                            nodes.add(new DynamoDBTableNode(tableName));
                        }
                    } while ( listTables.getLastEvaluatedTableName() != null );

                    return nodes.toArray();
                }
            }.start();
        }

        return Loading.LOADING;
    }

    @Override
    public String getServiceAbbreviation() {
        return ServiceAbbreviations.DYNAMODB;
    }

}

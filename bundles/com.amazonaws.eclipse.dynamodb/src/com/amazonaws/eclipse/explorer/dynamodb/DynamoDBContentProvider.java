package com.amazonaws.eclipse.explorer.dynamodb;

import java.util.LinkedList;
import java.util.List;

import org.eclipse.swt.widgets.Display;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.eclipse.core.AwsToolkitCore;
import com.amazonaws.eclipse.core.regions.ServiceAbbreviations;
import com.amazonaws.eclipse.explorer.AWSResourcesRootElement;
import com.amazonaws.eclipse.explorer.AbstractContentProvider;
import com.amazonaws.eclipse.explorer.Loading;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.model.DescribeTableRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesRequest;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.TableStatus;


public class DynamoDBContentProvider extends AbstractContentProvider {

    private static final long tableStatusRefreshDelay = 5 * 1000;
    
    private static DynamoDBContentProvider instance;
    
    public DynamoDBContentProvider() {
        /* Sets the background job that updates the table status. */
        setBackgroundJobFactory(new BackgroundContentUpdateJobFactory() {
            
            @Override
            protected synchronized boolean executeBackgroundJob(final Object parentElement) throws AmazonClientException {
                if ( null == cachedResponses || cachedResponses.isEmpty() || null == cachedResponses.get(parentElement) ) {
                    return false;
                }
                
                AmazonDynamoDB dynamoDBClient = AwsToolkitCore.getClientFactory().getDynamoDBV2Client();
                Object[] nodes = cachedResponses.get(parentElement);
                
                boolean refreshUI = false;
                boolean shouldKeepRunning = false;
                
                for ( Object node : nodes ) {
                    DynamoDBTableNode dynamoDBNode;
                    if ( node instanceof DynamoDBTableNode ) {
                        dynamoDBNode = (DynamoDBTableNode) node;
                    } else {
                        continue;
                    }
                    if ( dynamoDBNode.getTableStatus() != TableStatus.ACTIVE ) {
                        TableStatus updatedStatus;
                        try {
                            updatedStatus = TableStatus.valueOf(dynamoDBClient
                                    .describeTable(
                                            new DescribeTableRequest()
                                                    .withTableName(dynamoDBNode
                                                            .getTableName())).getTable()
                                    .getTableStatus());
                        } catch ( AmazonServiceException ase ) {
                            if (ase.getErrorCode().equalsIgnoreCase(
                                    "ResourceNotFoundException") == true) {
                                /* Refresh both the cache and UI when a table node has already been deleted. */
                                refresh();
                                return false;
                            } else {
                                throw ase;
                            }
                        } catch ( IllegalArgumentException iae ) {
                            throw new AmazonClientException("Unrecognized table status string.", iae);
                        }
                        
                        /* Only refresh UI when some status has changed */
                        if ( updatedStatus != dynamoDBNode.getTableStatus() ) {
                            dynamoDBNode.setTableStatus(updatedStatus);
                            refreshUI = true;
                        }
                        if ( updatedStatus != TableStatus.ACTIVE ) {
                            shouldKeepRunning = true;
                        }
                    }
                }
                if ( refreshUI ) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            viewer.refresh(parentElement);
                        }
                    });
                }
                return shouldKeepRunning;
            }

            @Override
            protected long getRefreshDelay() {
                return tableStatusRefreshDelay;
            }
        });
        instance = this;
    }
    
    public static DynamoDBContentProvider getInstance() {
        return instance;
    }

    @Override
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
                    List<DynamoDBTableNode> nodes = new LinkedList<>();
                    ListTablesResult listTables = new ListTablesResult();
                    do {
                        listTables = db.listTables(new ListTablesRequest().withExclusiveStartTableName(listTables
                                .getLastEvaluatedTableName()));
                        for ( String tableName : listTables.getTableNames() ) {
                            /* Defer getting the table status */
                            nodes.add(new DynamoDBTableNode(tableName, null));
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

package com.reinhardtvanwyk;

import com.amazonaws.services.cloudfront.AmazonCloudFront;
import com.amazonaws.services.cloudfront.AmazonCloudFrontClient;
import com.amazonaws.services.cloudfront.model.CreateInvalidationRequest;
import com.amazonaws.services.cloudfront.model.InvalidationBatch;
import com.amazonaws.services.cloudfront.model.Paths;
import com.amazonaws.services.codepipeline.AWSCodePipeline;
import com.amazonaws.services.codepipeline.AWSCodePipelineClient;
import com.amazonaws.services.codepipeline.model.FailureDetails;
import com.amazonaws.services.codepipeline.model.FailureType;
import com.amazonaws.services.codepipeline.model.PutJobFailureResultRequest;
import com.amazonaws.services.codepipeline.model.PutJobSuccessResultRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.reinhardtvanwyk.mapper.ArgumentRetriever;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.List;
import java.util.Map;

public class CFCacheInvalidatorLambdaHandler implements RequestHandler<Map<String,Object>, String> {

    private static Logger logger = LogManager.getLogger(CFCacheInvalidatorLambdaHandler.class);

    @Override
    public String handleRequest(Map<String,Object> event, Context context) {
        String response = "200 OK";
        ArgumentRetriever argumentRetriever = new ArgumentRetriever(event);
        try {
            String userParameters = argumentRetriever.getUserParameters();
            logger.info("Argument from initiatior: {}", userParameters);
            clearCache(argumentRetriever);
            passCompleteToCodePipeline(argumentRetriever);
        } catch (Exception e) {
            logger.error(e);
            passFailToCodePipeline(argumentRetriever);
        }
        return response;
    }

    private void clearCache(ArgumentRetriever argumentRetriever) {
        logger.info("Clearing caches from CloudFront");
        AmazonCloudFront cloudFront = AmazonCloudFrontClient.builder().build();
        cloudFront.createInvalidation(buildCreateInvalidationRequest(argumentRetriever));
        logger.info("Done clearing caches from CloudFront");
    }

    private Paths buildPaths(ArgumentRetriever argumentRetriever) {
        Paths paths = new Paths();
        List<String> items = argumentRetriever.getItems();
        paths.withItems(items);
        paths.setQuantity(items.size());
        return paths;
    }

    private InvalidationBatch buildInvalidationBatch(ArgumentRetriever argumentRetriever) {
        InvalidationBatch invalidationBatch = new InvalidationBatch();
        invalidationBatch.setPaths(buildPaths(argumentRetriever));
        invalidationBatch.setCallerReference(String.valueOf(System.currentTimeMillis()));
        return invalidationBatch;
    }

    private CreateInvalidationRequest buildCreateInvalidationRequest(ArgumentRetriever argumentRetriever) {
        CreateInvalidationRequest createInvalidationRequest = new CreateInvalidationRequest();
        createInvalidationRequest.setDistributionId(argumentRetriever.getDistributionId());
        createInvalidationRequest.setInvalidationBatch(buildInvalidationBatch(argumentRetriever));
        return createInvalidationRequest;
    }

    private void passCompleteToCodePipeline(ArgumentRetriever argumentRetriever) {
        AWSCodePipeline awsCodePipeline = AWSCodePipelineClient.builder().build();
        PutJobSuccessResultRequest putJobSuccessResultRequest = new PutJobSuccessResultRequest();
        putJobSuccessResultRequest.setJobId(argumentRetriever.getId());
        awsCodePipeline.putJobSuccessResult(putJobSuccessResultRequest);
    }

    private void passFailToCodePipeline(ArgumentRetriever argumentRetriever) {
        AWSCodePipeline awsCodePipeline = AWSCodePipelineClient.builder().build();
        PutJobFailureResultRequest putJobFailureResultRequest = new PutJobFailureResultRequest();
        putJobFailureResultRequest.setJobId(argumentRetriever.getId());
        FailureDetails failureDetails = new FailureDetails();
        failureDetails.setMessage("Clearing of cache failed");
        failureDetails.setType(FailureType.JobFailed);
        putJobFailureResultRequest.setFailureDetails(failureDetails);
        awsCodePipeline.putJobFailureResult(putJobFailureResultRequest);
    }

}

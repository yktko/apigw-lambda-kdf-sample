package example;
import com.amazonaws.services.lambda.runtime.Context; 
import com.amazonaws.services.lambda.runtime.RequestHandler;

import java.time.OffsetDateTime;

import java.nio.ByteBuffer;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehose;
import com.amazonaws.services.kinesisfirehose.AmazonKinesisFirehoseClientBuilder;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchRequest;
import com.amazonaws.services.kinesisfirehose.model.PutRecordBatchResult;
import com.amazonaws.services.kinesisfirehose.model.PutRecordRequest;
import com.amazonaws.services.kinesisfirehose.model.Record;

public class HelloPojo implements RequestHandler<RequestClass, ResponseClass>{ 

    private static String deliveryStreamName = "APIGWHandsOnStream";
    private static AmazonKinesisFirehose firehoseClient;

    private void putRecordIntoDeliveryStream(String line) {
        PutRecordRequest putRecordRequest = new PutRecordRequest();
        putRecordRequest.setDeliveryStreamName(deliveryStreamName);

        String data = line + "\n";
        Record record = new Record().withData(ByteBuffer.wrap(data.getBytes()));
        putRecordRequest.setRecord(record);

        // Put record into the DeliveryStream
        firehoseClient.putRecord(putRecordRequest);
    }

    public ResponseClass handleRequest(RequestClass request, Context context){

        String time = OffsetDateTime.now().toString();
        firehoseClient = AmazonKinesisFirehoseClientBuilder.standard().build();
        putRecordIntoDeliveryStream(time + "," + request.values);

        String greetingString = String.format("Hello %s, %s. time=%s, values=%s", request.firstName, request.lastName, time, request.values);
        return new ResponseClass(greetingString);
    }
}



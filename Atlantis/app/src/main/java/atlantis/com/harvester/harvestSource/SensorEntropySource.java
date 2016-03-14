package atlantis.com.harvester.harvestSource;

import android.app.Service;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;

/**
 * Created by Daniel on 5/27/2015.
 * Collects entropy from the sensor specified by sensorType
 */

public class SensorEntropySource implements HarvestSource, SensorEventListener {

    private static final int SIZE_OF_FLOAT = 4;
    private final ByteArrayOutputStream mBytesToOutput;
    private final SensorManager mSensorManager;
    private final int mSensorType;

    public SensorEntropySource(Service harvester, int sensorType) throws HarvestSourceNotSupportedException {
        mBytesToOutput = new ByteArrayOutputStream();
        mSensorType = sensorType;
        mSensorManager = (SensorManager) harvester.getSystemService(Context.SENSOR_SERVICE);

        if((mSensorManager == null) || (mSensorManager.getDefaultSensor(mSensorType) == null)) {
            throw new HarvestSourceNotSupportedException("A sensor of this type was not found on this device.");
        }
    }

    protected byte[] retrieveSensorValues(SensorEvent event) {

        //create a buffer to hold the sensor's values; event.values is a float array
        ByteBuffer sensorDataBuffer = ByteBuffer.allocate(SIZE_OF_FLOAT * event.values.length);

        for (int i = 0; i < event.values.length; i++) {
            //copy each sensor value into the byte buffer
            sensorDataBuffer.putFloat(event.values[i]);
        }

        //return the byte buffer as a float array
        return sensorDataBuffer.array();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        //get the sensor data as byte array
        byte[] sensorDataAsBytes = retrieveSensorValues(event);
        //write sensor data into class-level byte output stream
        mBytesToOutput.write(sensorDataAsBytes, 0, sensorDataAsBytes.length);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    public byte[] getBytesFromSource() {

        //get the bytes to output as a byte array
        byte[] outBytes = mBytesToOutput.toByteArray();

        //clear all currently accumulated data from output stream's internal buffer
        mBytesToOutput.reset();
        return outBytes;
    }

    public void startCollecting() {
        //to start collecting entropy, register the sensor listener
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(mSensorType), SensorManager.SENSOR_DELAY_FASTEST);
    }

    public void stopCollectingData() {
        //stop collecting entropy by simply unregistering the listener
        mSensorManager.unregisterListener(this, mSensorManager.getDefaultSensor(mSensorType));
    }
}
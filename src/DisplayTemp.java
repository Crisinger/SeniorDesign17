/**
 * Created by Charlie on 9/24/2017.
 */


import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import jssc.SerialPortList;
import javafx.scene.*;

import java.util.logging.Level;
import java.util.logging.Logger;

import static jssc.SerialPort.MASK_RXCHAR;

public class DisplayTemp extends Application {

    SerialPort arduinoPort = null;
    ObservableList<String> portList;

    Label tempValue;
    final int NUM_OF_POINTS = 300;
    XYChart.Series series;

    /*private void detectPort(){
        portList = FXCollections.observableArrayList();
        String[] serialPortNames = SerialPortList.getPortNames();
        for (String name: serialPortNames){
            System.out.println(name);
            portList.add(name);
        }
    }*/



    @Override
    public void start(Stage primaryStage){
        tempValue = new Label();

        /*detectPort();
        final ComboBox ports = new ComboBox(portList);
        ports.valueProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldVal, String newVal){
                disconnectArduino();
                connectArduino(newVal);
            }
        });*/
        //DEFINE COM PORT
        connectArduino("COM1");

        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Temp (C)");
        yAxis.setSide(Side.RIGHT);
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(10.0);
        yAxis.setUpperBound(50.0);
        yAxis.setMinorTickCount(10);



        xAxis.setLabel("seconds ago from current time");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0);
        xAxis.setUpperBound(300);
        xAxis.setMinorTickCount(50);



        final LineChart<Number,Number> lineChart = new LineChart<>(xAxis,yAxis);
        lineChart.setTitle("Temperature Reading");
        series = new XYChart.Series();
        series.setName("Temp output");
        lineChart.getData().add(series);
        lineChart.setAnimated(false);

        for (int i=0; i<NUM_OF_POINTS;i++){
            series.getData().add(new XYChart.Data(i,0));
        }
        VBox vbox = new VBox();
        vbox.getChildren().addAll(tempValue,lineChart);
        StackPane root = new StackPane();
        root.getChildren().add(vbox);
        Scene scene = new Scene(root,500,400);
        primaryStage.setScene(scene);
        primaryStage.show();



    }

    public void shiftData(float newValue){
        for (int i = 0; i < NUM_OF_POINTS-1;i++){
            XYChart.Data<String,Number> ShiftDataUp = (XYChart.Data<String,Number>)series.getData().get(i+1);
            Number shiftVal = ShiftDataUp.getYValue();
            XYChart.Data<String,Number> ShiftDataDn = (XYChart.Data<String,Number>)series.getData().get(i);
            ShiftDataDn.setYValue(shiftVal);
        }
        XYChart.Data<String,Number> lastData = (XYChart.Data<String,Number>)series.getData().get(NUM_OF_POINTS-1);
        lastData.setYValue(newValue);
    }

    public boolean connectArduino(String port){

        System.out.println("connectArduino");

        boolean success = false;
        SerialPort serialPort = new SerialPort(port);
        try {
            serialPort.openPort();
            //TODO verify these params w/ matt
            serialPort.setParams(
                    SerialPort.BAUDRATE_9600,
                    SerialPort.DATABITS_8,
                    SerialPort.STOPBITS_1,
                    SerialPort.PARITY_NONE);
            serialPort.setEventsMask(MASK_RXCHAR);
            serialPort.addEventListener((SerialPortEvent serialPortEvent) -> {
                if(serialPortEvent.isRXCHAR()){
                    try {

                        byte[] b = serialPort.readBytes();
                        int value = b[0] & 0xff;    //convert to int
                        String st = String.valueOf(value);
                        //TODO INTERPRET DATA FROM HARDWARE HERE
                        //Update label in ui thread
                        Platform.runLater(() -> {
                            tempValue.setText(st);
                            shiftData((float)value * 5/255); //in 5V scale
                        });
                        //TODO update label in ui Thread

                    } catch (SerialPortException ex) {
                        Logger.getLogger(DisplayTemp.class.getName())
                                .log(Level.SEVERE, null, ex);
                    }

                }
            });

            arduinoPort = serialPort;
            success = true;
        } catch (SerialPortException ex) {
            Logger.getLogger(DisplayTemp.class.getName())
                    .log(Level.SEVERE, null, ex);
            System.out.println("SerialPortException: " + ex.toString());
        }

        return success;
    }

    public void disconnectArduino(){

        System.out.println("disconnectArduino()");
        if(arduinoPort != null){
            try {
                arduinoPort.removeEventListener();

                if(arduinoPort.isOpened()){
                    arduinoPort.closePort();
                }

            } catch (SerialPortException ex) {
                Logger.getLogger(DisplayTemp.class.getName())
                        .log(Level.SEVERE, null, ex);
            }
        }
    }
    public static void main(String[] args) {
        launch(args);
    }
}

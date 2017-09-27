/**
 * Created by Charlie on 9/24/2017.
 */


import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Side;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortException;
import jssc.SerialPortList;
import javafx.scene.*;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.lang.Thread.sleep;
import static jssc.SerialPort.MASK_RXCHAR;

public class DisplayTemp extends Application {

    SerialPort arduinoPort = null;

    Label tempValue;
    final int NUM_OF_POINTS = 300;
    XYChart.Series series;
    ToggleButton LEDpower;
    int currentTemp = 30;
    int maxTemp = 55;
    int minTemp = 0;
    String phoneNumber;
    TextField minTempInput, maxTempInput, phoneNumberInput;



    @Override
    public void start(Stage primaryStage){
        tempValue = new Label();
        tempValue.setFont(new Font("TimesRoman",26));
        //Connect to the arduino uno port
        connectArduino("COM3");





        //Set up scene for display

        //Set up axis
        final NumberAxis xAxis = new NumberAxis();
        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Temp (C)");
        yAxis.setSide(Side.RIGHT);
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(10.0);
        yAxis.setUpperBound(50.0);
        yAxis.setTickUnit(10);


        xAxis.setLabel("seconds ago from current time");
        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(0.0);
        xAxis.setUpperBound(300.0);
        xAxis.setMinorTickCount(0);
        xAxis.setTickUnit(100);

        //set up Text fields
        minTempInput = new TextField();
        minTempInput.setPromptText("Enter Min Temperature");
        minTempInput.setEditable(true);
        minTempInput.setLayoutX(150.0);
        minTempInput.setLayoutY(453.0);

        maxTempInput = new TextField();
        maxTempInput.setPromptText("Enter Max Temperature");
        maxTempInput.setEditable(true);
        maxTempInput.setLayoutX(150.0);
        maxTempInput.setLayoutY(478.0);

        phoneNumberInput = new TextField();
        phoneNumberInput.setPromptText("Enter Phone Number");
        phoneNumberInput.setEditable(true);
        phoneNumberInput.setLayoutX(150.0);
        phoneNumberInput.setLayoutY(504.0);


        //set up button
        LEDpower = new ToggleButton("LED Power");
        LEDpower.setOnAction((ActionEvent event) -> {
            toggleLED();
            //addOneValTest();
        });


        //set up line chart
        final LineChart<Number,Number> lineChart = new LineChart<>(xAxis,yAxis);
        lineChart.setTitle("Temperature Reading");
        series = new XYChart.Series();
        lineChart.getData().add(series);
        lineChart.setAnimated(false);
        lineChart.setPrefSize(1000,500);




        for (int j=0; j<NUM_OF_POINTS;j++){
            series.getData().add(new XYChart.Data(j,0));
        }
        VBox vbox = new VBox();
        vbox.getChildren().addAll(tempValue,LEDpower,lineChart,minTempInput,maxTempInput,phoneNumberInput);
        StackPane root = new StackPane();
        root.getChildren().add(vbox);
        Scene scene = new Scene(root,1200,600);
        primaryStage.setScene(scene);
        primaryStage.show();



        /*
            Run tests and further monitor functions here

         */
        //FillGraphTest();


    }



    /*
    TESTS
     */

    public void addOneNullTest(){

        shiftData(-1);
        tempValue.setText("NA C"); //sets the label
    }
    public void addOneValTest(){
        Random rand = new Random();
        int randNum = rand.nextInt(8);
        int tf = rand.nextInt(2);
        if (tf == 0) {
            shiftData(currentTemp + randNum);
            currentTemp = currentTemp + randNum;
        } else{
            shiftData(currentTemp - randNum);
            currentTemp = currentTemp - randNum;
        }

        tempValue.setText(String.valueOf(currentTemp) + " C"); //sets the label
    }

    public void FillGraphTest(){
        for(int i=0;i<300;i++) { //fills the graph with random points
            addOneValTest();
        }
    }









    /*
    FUNCTIONS
     */

    private void setLabel(){
        tempValue.setText(String.valueOf(currentTemp));
    }


    private void setVals(){
        if (!maxTempInput.getText().equals("")){
            maxTemp = Integer.valueOf(maxTempInput.getText());
        }
        if (!phoneNumberInput.getText().equals("")) {
            phoneNumber = phoneNumberInput.getText();

        }
        if (!minTempInput.getText().equals("")) {
            minTemp = Integer.valueOf(minTempInput.getText());

        }

    }
    private void checkCriticalTemp(){
        if (currentTemp > maxTemp){
            sendTextMessage();
        } else if (currentTemp < minTemp){
            sendTextMessage();
        }
    }


    private void toggleLED(){
        try {
            if (LEDpower.isSelected()){
                for (int i = 0;i<50;i++) arduinoPort.writeByte((byte)0x01); //led on
            } else {
                for (int i = 0;i<50;i++) arduinoPort.writeByte((byte)0x02); //led off
            }
        } catch (SerialPortException ex) {
            ex.printStackTrace();

        }
    }

    public void shiftData(float newValue){
        for (int i = 0; i < NUM_OF_POINTS-1;i++){
            XYChart.Data<String,Number> PreviousPoint = (XYChart.Data<String,Number>)series.getData().get(i+1);
            Number tempVal = PreviousPoint.getYValue();
            XYChart.Data<String,Number> CurrentPoint = (XYChart.Data<String,Number>)series.getData().get(i);
            CurrentPoint.setYValue(tempVal);
        }
        XYChart.Data<String,Number> lastData = (XYChart.Data<String,Number>)series.getData().get(NUM_OF_POINTS-1);
        lastData.setYValue(newValue);

    }



    public boolean connectArduino(String port) {

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
                        String s = serialPort.readString(2);
                        int value = Integer.valueOf(s);
                        /*
                        THIS CODE WILL EXECUTE EVERY TIME WE RECEIVE A BIT. THIS IS ESSENTIALLY THE LOOP
                         */
                        Platform.runLater(() -> {
                            System.out.println(s);
                            shiftData(value);
                            setVals(); //set the critical temps to new values
                            checkCriticalTemp();
                            currentTemp = value;
                            setLabel();
                            toggleLED();
                        });


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

    private void sendTextMessage(){
        //TODO Colleen
        System.out.println("text message sent");
    }
    public static void main(String[] args) {
        launch(args);
    }
}

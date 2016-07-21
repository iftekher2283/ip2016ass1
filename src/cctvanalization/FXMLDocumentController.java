/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cctvanalization;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import sun.audio.AudioData;
import sun.audio.AudioPlayer;
import sun.audio.AudioStream;
import sun.audio.ContinuousAudioDataStream;


/**
 *
 * @author iftekher
 */
public class FXMLDocumentController implements Initializable {
    
    private static boolean applicationShouldClose = false;

    static {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
    }

    @FXML
    private ImageView cameraView;
    private VideoCapture videoCapture;
    private ScheduledExecutorService scheduledExecutorService;
    
    private List<Mat> grabbedFramesPrev;
    private List<Mat> grabbedFramesTemp;
    private List<Mat> grabbedFramesNext;
    
    private Mat currentFrame;
    private Mat previousFrame;
    
    private InputStream in;
    private AudioStream as;
    private AudioData data;
    private ContinuousAudioDataStream cas;
    
    private int alarmCont = 0;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        videoCapture = new VideoCapture();
        grabbedFramesPrev = new ArrayList<>();
        grabbedFramesNext = new ArrayList<>();
        grabbedFramesTemp = new ArrayList<>();
        currentFrame = new Mat();
        previousFrame = new Mat();
        
        Timeline timeline = new Timeline();
        KeyFrame keyFrame = new KeyFrame(Duration.millis(10), event -> {
            if(grabbedFramesPrev.size() > 1){
            System.out.println("Frames found");
            currentFrame = grabbedFramesPrev.get(grabbedFramesPrev.size() - 1);
            previousFrame = grabbedFramesPrev.get(grabbedFramesPrev.size() - 2);
            
//            Mat subtraction = new Mat();
//            System.out.println("Mat created");
            
            for(int r = 0; r < currentFrame.height(); r++){
                for(int c = 0; c < currentFrame.width(); c++){
                    double currentValue[] = currentFrame.get(r, c);
                    double previousValue[] = previousFrame.get(r, c);
                    double newValue = currentValue[0] - previousValue[0];
                    if(newValue < 0){
                        newValue = 0;
                    }
                //    System.out.printf("%f   ", newValue);
           //         subtraction.put(r, c, newValue);
                    
                    if (newValue > 10){
                        int changes = 0;
                        for(int i = r-10; i <= r; i++){
                            for(int j = c-10; j <= c; j++){
                                if(i >= 0 && j >=0){
                                    double currCheckValue[] = currentFrame.get(i, j);
                                    double prevCheckValue[] = previousFrame.get(i, j);
                                    double checkValue = currCheckValue[0] - prevCheckValue[0];
                                    if(checkValue < 0){
                                        checkValue = 0;
                                    }
                                    if(checkValue > 10){
                                        changes = changes + 1;
                                    }
                                }
                            }
                        }
                        if (changes > 40){
                            System.out.println("ChangeFound");
                            if(alarmCont == 0){
                                alarmCont = 1;
                            }
                            if(alarmCont == 1){
                                ringAlarm();
                                alarmCont = 2;
                            }
                            break;
                        }
                    }   
                }
             //   System.out.printf("\n");
            }
        }
        });
        timeline.getKeyFrames().add(keyFrame);
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handleStartCameraAction(ActionEvent event) {
        videoCapture.open(0);
        scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();
        scheduledExecutorService.scheduleAtFixedRate(() -> cameraView.setImage(grabFrame()), 0, 10, TimeUnit.MILLISECONDS);
    }
    
    private Image grabFrame() {
        if (applicationShouldClose) {
            if (videoCapture.isOpened()) {
                videoCapture.release();
            }
            scheduledExecutorService.shutdown();
        }
        
        Image imageToShow = null;
        Mat frame = new Mat();
   //     Mat prevFrame = new Mat(grabbedImagesPrev.get(grabbedImagesPrev.size() - 1));
        int frameNum = 0;
        if (videoCapture.isOpened()) {
            try {
                videoCapture.read(frame);

                if (!frame.empty()) {
                    Imgproc.cvtColor(frame, frame, Imgproc.COLOR_BGR2GRAY);
                    
                    MatOfByte buffer = new MatOfByte();
                    Imgcodecs.imencode(".png", frame, buffer);
                    imageToShow = new Image(new ByteArrayInputStream(buffer.toArray()));
                    
                    grabbedFramesTemp.removeAll(grabbedFramesTemp);
                    if(grabbedFramesPrev.size() < 10){
                        grabbedFramesPrev.add(frame);
                    }
                    else{
                        for (int i = 1; i < grabbedFramesPrev.size(); i++){
                            grabbedFramesTemp.add(grabbedFramesPrev.get(i));
                        } 
                        grabbedFramesPrev.removeAll(grabbedFramesPrev);
                        for (int i = 0; i < grabbedFramesTemp.size(); i++){
                            grabbedFramesPrev.add(grabbedFramesTemp.get(i));
                        }
                        grabbedFramesPrev.add(frame);
                    }
                }

            } catch (Exception e) {
                System.err.println(e);
            }
        }

        return imageToShow;
    }

    @FXML
    private void handleStopCameraAction(ActionEvent event) {
        if (videoCapture.isOpened()) {
            videoCapture.release();
        }
        scheduledExecutorService.shutdown();
    }

    public static void exit() {
        applicationShouldClose = true;
    }

            
//            System.out.println("Mat Value Put");
//            String change = detectChange(subtraction);
//            if (change.equals("Yes")){
//                System.out.println("Change found");
//            }
//            int changes = 0;
//            System.out.println("Changes: " + changes);
//            
//            for(int r = 0; r < subtraction.height(); r++){
//                for(int c = 0; c < subtraction.width(); c++){
//                    System.out.println("Started change detecting");
//
//                    double value[] = subtraction.get(r, c);
//                    System.out.println(value[0]);
//                    if(value[0] > 0){
////                        System.out.println("Initial Change Detected");
////                        for(int i = r-5; i < r + 5; i++){
////                            for(int j = c-5; c < c+5; c++){
////                                double checkValue[] = subtraction.get(i, j);
////                                if(checkValue[0] > 2){
////                                    changes = changes + 1;
////                                }
////                            }
////                        }
//                        changes = changes + 1;
//                    }
//                }
//            }
//            
//            if(changes > 5){
//                System.out.println("Change Detected");
//                
//                MatOfByte buffer = new MatOfByte();
//                Imgcodecs.imencode(".png", subtraction, buffer);
//                BufferedImage imageToSave = new BufferedImage(subtraction.width(), subtraction.height(), BufferedImage.TYPE_INT_ARGB);
//                try {
//                    FileChooser fileChooser = new FileChooser();
//                    File file = fileChooser.showSaveDialog(null);
//                    String format = file.getName().substring(file.getName().indexOf(".") + 1);
//                    ImageIO.write(imageToSave, format, file);
//                 //   statusLabel.setText("Saving to " + file.getName() + " format: " + format);
//                } catch (IOException ex) {
//                    Logger.getLogger(FXMLDocumentController.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            }
      //  }
//    }
    
    public void ringAlarm(){
        try {
            in = new FileInputStream("alarm/CoastGuard.wav");
            as = new AudioStream(in);
            data = as.getData();
            cas = new ContinuousAudioDataStream(data);
            AudioPlayer.player.start(cas);
        } catch (FileNotFoundException ex) {
            Logger.getLogger(CctvAnalization.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(CctvAnalization.class.getName()).log(Level.SEVERE, null, ex);
        }
     }

    @FXML
    private void handleStopAlarmAction(ActionEvent event) {
        alarmCont = 0;
        AudioPlayer.player.start(cas);
    }
    
    public void saveFootage(){
        
    }
    
    public void sendEmail(){
        
    }
     
}

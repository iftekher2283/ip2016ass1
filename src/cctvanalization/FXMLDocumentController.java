/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package cctvanalization;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;

/**
 *
 * @author student
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
    
    private List<Image> grabbedImagesPrev;
    private List<Image> grabbedImagesNext;
    private List<Image> grabbedImagesTemp;
    
    private BufferedImage currentFrame;
    private BufferedImage previousFrame;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        videoCapture = new VideoCapture();
        grabbedImagesPrev = new ArrayList<>();
        grabbedImagesNext = new ArrayList<>();
        grabbedImagesTemp = new ArrayList<>();
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
        Mat currentFrame = new Mat();
   //     Mat prevFrame = new Mat(grabbedImagesPrev.get(grabbedImagesPrev.size() - 1));
        int frameNum = 0;
        if (videoCapture.isOpened()) {
            try {
                videoCapture.read(currentFrame);

                if (!currentFrame.empty()) {
                    Imgproc.cvtColor(currentFrame, currentFrame, Imgproc.COLOR_BGR2GRAY);
                    
                    MatOfByte buffer = new MatOfByte();
                    Imgcodecs.imencode(".png", currentFrame, buffer);
                    imageToShow = new Image(new ByteArrayInputStream(buffer.toArray()));
                    grabbedImagesTemp.removeAll(grabbedImagesTemp);
                    if(grabbedImagesPrev.size() < 10){
                        grabbedImagesPrev.add(imageToShow);
                    }
                    else{
                        for (int i = 1; i < grabbedImagesPrev.size(); i++){
                            grabbedImagesTemp.add(grabbedImagesPrev.get(i));
                        } 
                        for (int i = 0; i < grabbedImagesTemp.size(); i++){
                            grabbedImagesPrev.add(grabbedImagesTemp.get(i));
                        }
                        grabbedImagesPrev.add(imageToShow);
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
    
}

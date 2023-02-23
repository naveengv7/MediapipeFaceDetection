package com.bagus.mediapipefacedetection;

//Link for Point Information
//https://raw.githubusercontent.com/google/mediapipe/a908d668c730da128dfa8d9f6bd25d519d006692/mediapipe/modules/face_geometry/data/canonical_face_model_uv_visualization.png


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.media.FaceDetector;

import com.google.mediapipe.formats.proto.LandmarkProto;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class IrisData {

    //Image Dimensions for Normalization
    private final int DEFAULT_WIDTH;
    private final int DEFAULT_HEIGHT;

    //Iris offset in a landmark list from this model
    private final int IRIS_DATA_LOCATION_OFFSET = 468;

    //Iris Location Data
    private List<Point2D> irisLocations;
    private Bitmap FaceImage;

    //External Iris Markers
    public static final int CENTER_IRIS_R = 0;
    public static final int LEFT_IRIS_R = 1;
    public static final int BOTTOM_IRIS_R = 2;
    public static final int RIGHT_IRIS_R = 3;
    public static final int TOP_IRIS_R = 4;
    public static final int CENTER_IRIS_L = 5;
    public static final int RIGHT_IRIS_L = 6;
    public static final int BOTTOM_IRIS_L = 7;
    public static final int LEFT_IRIS_L = 8;
    public static final int TOP_IRIS_L = 9;


    public IrisData(List<LandmarkProto.NormalizedLandmark> landmarkList, Bitmap image)
    {
        //Saving the Image
        FaceImage = image;

        //Setting the Height and Width for Unnormalization
        DEFAULT_WIDTH = FaceImage.getWidth(); DEFAULT_HEIGHT = FaceImage.getHeight();

        System.out.println("Height: " + DEFAULT_HEIGHT);
        System.out.println("Width: " + DEFAULT_WIDTH);

        //Initializing irisLocations
        irisLocations = new ArrayList<Point2D>();

        //Loading data into the List
        for(int i = 0;i < 10;i++)
        {
            irisLocations.add(new Point2D(
                    (int) (landmarkList.get(i + IRIS_DATA_LOCATION_OFFSET).getX() * DEFAULT_HEIGHT),
                    (int) (landmarkList.get(i + IRIS_DATA_LOCATION_OFFSET).getY() * DEFAULT_WIDTH)));


            System.out.println("(" + landmarkList.get(i + IRIS_DATA_LOCATION_OFFSET).getX() * DEFAULT_HEIGHT + "," + landmarkList.get(i + IRIS_DATA_LOCATION_OFFSET).getY() * DEFAULT_WIDTH + ")");
        }
    }

    public void generateIrisImages(String leftIrisFilename, String rightIrisFilename)
    {
        //Creating output files for the images, and deleting any existing images
        File leftIrisFile = new File(leftIrisFilename);
        File rightIrisFile = new File(rightIrisFilename);

        //Deleting output files if they exist
        if(leftIrisFile.exists()) leftIrisFile.delete();
        if(rightIrisFile.exists()) rightIrisFile.delete();

        //Getting the center of each iris
        int centerLX = this.getPoint(CENTER_IRIS_L).X;
        int centerLY = this.getPoint(CENTER_IRIS_L).Y;
        int centerRX = this.getPoint(CENTER_IRIS_R).X;
        int centerRY = this.getPoint(CENTER_IRIS_R).Y;

        //Setting the Iris Size
        int IrisSize = 600;

        //Getting the Corresponding Bitmaps
        Bitmap leftIris   = Bitmap.createBitmap(FaceImage, (DEFAULT_WIDTH - centerLY) - (IrisSize/2), centerLX - (IrisSize/2), IrisSize - 0, IrisSize - 0);
        Bitmap rightIris  = Bitmap.createBitmap(FaceImage, (DEFAULT_WIDTH - centerRY) - (IrisSize/2), centerRX - (IrisSize/2), IrisSize - 0, IrisSize - 0);
        
        //Saving Bitmaps
        try
        {
            leftIris.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(leftIrisFile));
            rightIris.compress(Bitmap.CompressFormat.PNG, 100, new FileOutputStream(rightIrisFile));
        }
        catch(FileNotFoundException e) {System.out.println(e);}
    }

    public void generateFile(String filename)
    {
        try
        {
            //Creating the filewriter
            FileWriter fileWriter = new FileWriter(filename);

            //Clearing the file
            fileWriter.write("");

            //Placing each entry into the file
            for(int i = 0;i < 10;i++)
            {
                fileWriter.append("(" + irisLocations.get(i).X + "," + irisLocations.get(i).Y + ")");
                if(i != 9) fileWriter.append(";");
            }

            //Closing the filewriter
            fileWriter.close();
        }
        catch (IOException e) {}
    }

    @Override
    public String toString()
    {
        String outputString = "";
        //Placing each entry into the file
        for(int i = 0;i < 10;i++)
        {
            outputString += "(" + irisLocations.get(i).X + "," + irisLocations.get(i).Y + ")";
            if(i != 9) outputString += ";\n";
        }

        return outputString;
    }

    public Point2D getPoint(int landmark)
    {
        return irisLocations.get(landmark);
    }

}

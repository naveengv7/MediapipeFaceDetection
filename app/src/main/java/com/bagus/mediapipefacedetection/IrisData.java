package com.bagus.mediapipefacedetection;

//Link for Point Information
//https://raw.githubusercontent.com/google/mediapipe/a908d668c730da128dfa8d9f6bd25d519d006692/mediapipe/modules/face_geometry/data/canonical_face_model_uv_visualization.png


import android.graphics.Point;

import com.google.mediapipe.formats.proto.LandmarkProto;

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

    //External Iris Markers
    public final int CENTER_IRIS_R = 0;
    public final int LEFT_IRIS_R = 1;
    public final int BOTTOM_IRIS_R = 2;
    public final int RIGHT_IRIS_R = 3;
    public final int TOP_IRIS_R = 4;
    public final int CENTER_IRIS_L = 5;
    public final int RIGHT_IRIS_L = 6;
    public final int BOTTOM_IRIS_L = 7;
    public final int LEFT_IRIS_L = 8;
    public final int TOP_IRIS_L = 9;

    public IrisData(LandmarkProto.NormalizedLandmarkList landmarkList)
    {
        //Defualt Image Dimensions for Normalization
        this(landmarkList, 1280, 720);
    }

    public IrisData(LandmarkProto.NormalizedLandmarkList landmarkList, int height, int width)
    {
        //Setting the Height and Width for Unnormalization
        DEFAULT_WIDTH = width; DEFAULT_HEIGHT = height;

        //Creating a local instance of the landmark list
        List<LandmarkProto.NormalizedLandmark> localLandmarkList = landmarkList.getLandmarkList();

        //Initializing irisLocations
        irisLocations = new ArrayList<Point2D>();

        //Loading data into the List
        for(int i = 0;i < 10;i++)
        {
            irisLocations.add(new Point2D(
                    (int) (localLandmarkList.get(i + IRIS_DATA_LOCATION_OFFSET).getX() * DEFAULT_WIDTH),
                    (int) (localLandmarkList.get(i + IRIS_DATA_LOCATION_OFFSET).getY() * DEFAULT_HEIGHT)));
        }
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

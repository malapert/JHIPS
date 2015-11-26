/*******************************************************************************
 * Copyright 2015 - Jean-Christophe Malapert (jcmalapert@gmail.com)
 *
 * This file is part of JHIPS.
 *
 * JHIPS is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * JHIPS is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * JHIPS. If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package io.github.malapert.jhips.metadata;

import io.github.malapert.jhips.algorithm.Tree;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Metadata for MAST camera.
 * <p>
 * The Mast Camera (Mastcam) is comprised of a pair of color-capable focusable
 * stereo cameras (“eyes”) mounted on the rover’s Remote Sensing Mast (RSM). For
 * the RSM, the "MEASURED" values (see ARTICULATION_DEVICE_ANGLE_NAME) represent
 * the value of the resolver (attached to the output side of the joint), while
 * the "FINAL" values represent the encoder (attached to the motor). The
 * resolver should be preferentially used if available, as it measures the angle
 * after joint backlash. A value of 1e+30 indicates the angle is not available,
 * in which case the encoder should be used instead. Note that the "INITIAL" and
 * "REQUESTED" values are also encoder measurements, and could be used to
 * determine the joint's direction of motion for backlash determination.
 *
 * @author Jean-Christophe Malapert
 */
public class MastPDSMetadata implements JHipsMetadataProviderInterface {

    private final static double NO_VALUE = 1e30;
    private final static String PDS_GROUP = "GROUP";
    private final static String PDS_END_GROUP = "END_GROUP";
    private final static String PDS_OBJECT = "OBJECT";
    private final static String PDS_END_OBJECT = "END_OBJECT";
    private final static String PDS_END = "END";
    private final static String PDS_RSM_ARTICULATION_STATE_PARMS = "RSM_ARTICULATION_STATE_PARMS";
    private final static String PDS_ARTICULATION_DEVICE_ANGLE = "ARTICULATION_DEVICE_ANGLE";
    private final static String PDS_INSTRUMENT_STATE_PARMS = "INSTRUMENT_STATE_PARMS";
    private final static String PDS_HORIZONTAL_FOV = "HORIZONTAL_FOV";
    private final static String PDS_VERTICAL_FOV = "VERTICAL_FOV";
    private final static String PDS_IMAGE_REQUEST_PARMS = "IMAGE_REQUEST_PARMS";
    private final static String PDS_LINES = "LINES";
    private final static String PDS_LINE_SAMPLES = "LINE_SAMPLES";
    private final static String PDS_DETECTOR_LINES = "DETECTOR_LINES";
    private final static String MSL_DETECTOR_SAMPLES = "MSL:DETECTOR_SAMPLES";
    private final static String PDS_FIRST_LINE_SAMPLE = "FIRST_LINE_SAMPLE";
    private final static String PDS_FIRST_LINE = "FIRST_LINE";
    private final static String PDS_INSTRUMENT_ID = "INSTRUMENT_ID";

    /**
     * Pixel size in mm along X and Y axis.
     */
    private final static double[] PIXEL_SIZE = new double[]{0.0074, 0.0074};

    /**
     * INSTRUMENT_ID, x0(mm), y0(mm), k1, k2, k3
     */
    private final static Map<String, double[]> DISTORTION_COEFF = new HashMap();

    static {
        DISTORTION_COEFF.put("MAST_LEFT", new double[]{-0.113876, 0.152029, -1.118977e-04, -1.023513e-06, 0.0});
        DISTORTION_COEFF.put("MAST_RIGHT", new double[]{0.262451, -0.250667, 1.513695e-04, 0.0, 0.0});

    }
    private final Tree metadata = new Tree("PDS");
    private final URL file;
    private String instrumentID;

    /**
     * Horizontal coordinates of the camera's center in radians.
     */
    private double[] horizontalCoordinates;

    /**
     * Camera's FOV [along longitude, along latitude] in radians.
     */
    private double[] cameraFov;

    /**
     * The real size of the image in pixels.
     * <p>
     * The requested image can be smaller or higher then the detector. This
     * depends on the acquisition mode.
     */
    private int[] subImage;

    /**
     * First line of the sub-Image in pixel coordinates.
     */
    public int[] firstLine;

    /**
     * Size of the detector in pixels.
     */
    private int[] detectorSize;

    /**
     * Creates an instance of PDS metadata.
     * <p>
     * For each PNG file, the associated LBL file is loaded and parsed to fill
     * the metadata.
     *
     * @param file PNG file
     * @throws IOException
     */
    public MastPDSMetadata(final URL file) throws IOException {
        Logger.getLogger(MastPDSMetadata.class.getName()).log(Level.INFO, "Parsing file {0}", file.getFile());
        this.file = file;
        String lblFile = file.getFile().replace("png", "LBL");
        init(new URL("file://" + lblFile));
    }

    /**
     * Fill the metadata structure.
     *
     * @param file the LBL file
     * @throws FileNotFoundException
     * @throws IOException
     */
    private void init(final URL file) throws FileNotFoundException, IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file.getFile()));
        String currentLine;
        while ((currentLine = reader.readLine()) != null) {
            currentLine = currentLine.trim();
            currentLine = skipComment(currentLine);
            if (currentLine.isEmpty()) {
                continue;
            }
            if (isEnd(currentLine)) {
            } else if (hasGroup(currentLine)) {
                parseGroup(currentLine, reader);
            } else if (hasObject(currentLine)) {
                parseObject(currentLine, reader);
            } else {
                String[] data = parseKeyword(currentLine, reader);
                this.metadata.addLeaf(data[0]).addLeaf(parseValue(data[1]));
            }
        }
        computeDetectorSize();
        computeFOV();
        computeFirstSample();
        computeHorizontalCoordinates();
        computeInstrumentID();
        computeSubImage();
    }

    /**
     * Converts PDS value.
     * <p>
     * A ( ,,, ) is transformed as an array. The \" are removed.
     *
     * @param data data to parse
     * @return the new value
     */
    private Object parseValue(String data) {
        Object result;
        if (data.startsWith("\"")) {
            result = data.replace("\"", "");
        } else if (data.startsWith("(")) {
            data = data.replace("(", "");
            data = data.replace(")", "");
            data = data.replace("\"", "");
            data = data.trim();
            String[] values = data.split(",");
            result = values;
        } else {
            result = data;
        }
        return result;
    }

    /**
     * Skips comments.
     *
     * @param currentLine the current ASCII line
     * @return the current line without comments.
     * @throws IOException
     */
    private String skipComment(final String currentLine) throws IOException {
        return (currentLine.startsWith("/*")) ? "" : currentLine;
    }

    /**
     * Checks if the currentLine is a group
     *
     * @param currentLine the current line
     * @return True when the current line is a group otherwise False.
     * @throws IOException
     */
    private boolean hasGroup(final String currentLine) throws IOException {
        return currentLine.startsWith(PDS_GROUP);
    }

    /**
     * Checks if the current line is an object
     *
     * @param currentLine the current line
     * @return True when the current line is an object otherwise False
     * @throws IOException
     */
    private boolean hasObject(final String currentLine) throws IOException {
        return currentLine.startsWith(PDS_OBJECT);
    }

    /**
     * Checks if the current line is the end of the group.
     *
     * @param currentLine the current line
     * @return True when the current line is the end of the group otherwise
     * False
     * @throws IOException
     */
    private boolean isEnd(final String currentLine) throws IOException {
        return currentLine.startsWith(PDS_END);
    }

    /**
     * Parses a group and adds elements in the metadata structure.
     *
     * @param currentLine the current line
     * @param reader the file reader
     * @throws IOException
     */
    private void parseGroup(String currentLine, final BufferedReader reader) throws IOException {
        String[] group = currentLine.split("\\s+=\\s+");
        Tree node = this.metadata.addLeaf(group[1]);
        while (!(currentLine = reader.readLine()).startsWith(PDS_END_GROUP)) {
            currentLine = currentLine.trim();
            String[] data = parseKeyword(currentLine, reader);
            node.addLeaf(data[0]).addLeaf(parseValue(data[1]));
        }
    }

    /**
     * Parses an object and adds elements in the metadata structure.
     *
     * @param currentLine the current line
     * @param reader the file reader
     * @throws IOException
     */
    private void parseObject(String currentLine, final BufferedReader reader) throws IOException {
        String[] object = currentLine.split("\\s+=\\s+");
        Tree node = this.metadata.addLeaf(object[1]);
        while (!(currentLine = reader.readLine()).startsWith(PDS_END_OBJECT)) {
            currentLine = currentLine.trim();
            String[] data = parseKeyword(currentLine, reader);
            node.addLeaf(data[0]).addLeaf(parseValue(data[1]));
        }
    }

    /**
     * Parses keywords.
     *
     * @param currentLine the current line
     * @param reader
     * @return the keyword/value
     * @throws IOException
     */
    private String[] parseKeyword(String currentLine, final BufferedReader reader) throws IOException {
        String[] keyword = currentLine.split("\\s+=\\s+");
        reader.mark(2048);
        currentLine = reader.readLine();
        while (!currentLine.contains("=")) {
            currentLine = currentLine.trim();
            currentLine = skipComment(currentLine);
            currentLine = currentLine.replace("\\s+", "");
            keyword[1] += currentLine;
            reader.mark(2048);
            currentLine = reader.readLine();
        }
        reader.reset();
        return keyword;
    }

    /**
     * Stores the horizontal coordinates.
     */
    protected void computeHorizontalCoordinates() {
        Tree coordinates = this.metadata.getTree(PDS_RSM_ARTICULATION_STATE_PARMS);
        String[] angles = (String[]) coordinates.getLastLeaf(PDS_ARTICULATION_DEVICE_ANGLE);
        double azimuth = Double.valueOf(angles[0].replace(" <rad>", ""));
        double elevation = Double.valueOf(angles[1].replace(" <rad>", "")) - Math.PI / 2;
        this.horizontalCoordinates = new double[]{azimuth, elevation};
    }

    @Override
    public double[] getHorizontalCoordinates() {
        return this.horizontalCoordinates;
    }

    /**
     * Stores the field of view.
     */
    protected void computeFOV() {
        Tree instrument = this.metadata.getTree(PDS_INSTRUMENT_STATE_PARMS);
        String azimuthAsString = (String) instrument.getLastLeaf(PDS_HORIZONTAL_FOV);
        String elevationAsString = (String) instrument.getLastLeaf(PDS_VERTICAL_FOV);
        double azimuth = Double.valueOf(azimuthAsString);
        double elevation = Double.valueOf(elevationAsString);
        this.cameraFov = new double[]{Math.toRadians(azimuth), Math.toRadians(elevation)};
    }

    @Override
    public double[] getFOV() {
        return this.cameraFov;
    }

    /**
     * Stores the sub-image size.
     */
    protected void computeSubImage() {
        Tree image = this.metadata.getTree(PDS_IMAGE_REQUEST_PARMS);
        String linesAsString = (String) image.getLastLeaf(PDS_LINES);
        String columnsAsString = (String) image.getLastLeaf(PDS_LINE_SAMPLES);
        int y = (linesAsString != null) ? Integer.parseInt(linesAsString) : 0;
        int x = (columnsAsString != null) ? Integer.parseInt(columnsAsString) : 0;
        this.subImage = new int[]{x, y};        
    }
    
    @Override
    public void setSubImageSize(int[] subImage) {
        this.subImage = subImage;
    }

    @Override
    public int[] getSubImageSize() {
        return this.subImage;
    }

    /**
     * Stores the detector size.
     */
    protected void computeDetectorSize() {
        Tree detector = this.metadata.getTree(PDS_INSTRUMENT_STATE_PARMS);
        String linesAsString = (String) detector.getLastLeaf(PDS_DETECTOR_LINES);
        String columnsAsString = (String) detector.getLastLeaf(MSL_DETECTOR_SAMPLES);
        this.detectorSize = new int[]{Integer.parseInt(columnsAsString), Integer.parseInt(linesAsString)};        
    }
    
    @Override
    public int[] getDetectorSize() {
        return this.detectorSize;
    }

    /**
     * Stores the firstSample.
     */
    protected void computeFirstSample() {
        Tree image = this.metadata.getTree(PDS_IMAGE_REQUEST_PARMS);
        String linesAsString = (String) image.getLastLeaf(PDS_FIRST_LINE);
        String columnsAsString = (String) image.getLastLeaf(PDS_FIRST_LINE_SAMPLE);
        int y = (linesAsString != null) ? Integer.parseInt(linesAsString) - 1 : 0;
        int x = (columnsAsString != null) ? Integer.parseInt(columnsAsString) - 1 : 0;
        this.firstLine = new int[]{x, y};        
    }
    
    @Override
    public int[] getFirstSample() {
        return this.firstLine;
    }

    @Override
    public URL getFile() {
        return this.file;
    }

    /**
     * Stores the instrumentID.
     */
    protected void computeInstrumentID() {
        this.instrumentID = (String) this.metadata.getTree("PDS").getLastLeaf(PDS_INSTRUMENT_ID);
    }
    
    @Override
    public String getInstrumentID() {
        return this.instrumentID;
    }

    @Override
    public Map<String, double[]> getDistortionCoeff() {
        return DISTORTION_COEFF;
    }

    @Override
    public double[] getPixelSize() {
        return PIXEL_SIZE;
    }

    /**
     * Main program.
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        MastPDSMetadata pds = new MastPDSMetadata(new URL("file:///home/malapert/Documents/MARS/PanoData/0900MR0039440010501309C00_DRCL.png"));
        double[] hor = pds.getHorizontalCoordinates();
        double[] fov = pds.getFOV();
        int[] image = pds.getSubImageSize();
        System.out.println("az = " + Math.toDegrees(hor[0]) + " elev=" + Math.toDegrees(hor[1]));
        System.out.println("fovAz = " + Math.toDegrees(fov[0]) + " fovEle=" + Math.toDegrees(fov[1]));
        System.out.println("image : " + image[0] + " x " + image[1]);
        System.out.println("instrumentID : " + pds.getInstrumentID());
    }
}

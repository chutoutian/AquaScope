package com.example.root.ffttest2;

import static com.example.root.ffttest2.Constants.LOG;
import static com.example.root.ffttest2.Constants.XCORR_MAX_VAL_HEIGHT_FAC;
import static com.example.root.ffttest2.Constants.currentDirPath;
import static com.example.root.ffttest2.Constants.fbackTime;
import static com.example.root.ffttest2.Constants.sample_num;
import static com.example.root.ffttest2.Constants.scheme;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.Context;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;
import android.os.BatteryManager;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.ByteArrayOutputStream;

import android.graphics.Color;
import android.widget.EditText;
import android.widget.ImageView;

import org.apache.commons.math3.complex.Complex;
import org.pytorch.IValue;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;


import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.stat.descriptive.rank.Max;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
import org.apache.commons.math3.stat.descriptive.rank.Median;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;
import org.apache.commons.math3.analysis.interpolation.LinearInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;
public class Utils {

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }

    // Convert a Bitmap to a binary string


    public static String convertByteArrayToBitString(byte[] byteArray) {
        StringBuilder bitString = new StringBuilder();
        for (byte b : byteArray) {
            // Convert each byte to an 8-character binary string
            String binaryString = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            bitString.append(binaryString);
        }
        return bitString.toString();
    }

    public static byte[] convertBitStringToByteArray(String bitString) {
        int byteLength = bitString.length() / 8;
        byte[] byteArray = new byte[byteLength];

        for (int i = 0; i < byteLength; i++) {
            int beginIndex = i * 8;
            int endIndex = (i + 1) * 8;
            String byteString = bitString.substring(beginIndex, endIndex);
            byte parsedByte = (byte) Integer.parseInt(byteString, 2);
            byteArray[i] = parsedByte;
        }

        return byteArray;
    }

    public static boolean[] byteToBinaryBooleanArray(byte[] input) {
        // Each byte will be converted into 8 boolean values, so the size of the output array
        // is input.length * 8
        boolean[] bitArray = new boolean[input.length * 8];

        for (int i = 0; i < input.length; i++) {
            for (int bit = 0; bit < 8; bit++) {
                // Shift each bit of the byte to the least significant position and mask with 1
                // to isolate the bit. The result is compared to 0 to get a boolean value.
                // Note: We start with the MSB by subtracting the bit index from 7.
                bitArray[i * 8 + bit] = ((input[i] >> (7 - bit)) & 1) != 0;
            }
        }

        return bitArray;
    }

    public static int[] nibbleToBinaryBooleanArray(byte[] input) {
        // Each byte will be converted into 8 boolean values, so the size of the output array
        // is input.length * 8
        //boolean[] bitArray = new boolean[input.length * 4];
        int[] intArray = new int[input.length * 4];

        for (int i = 0; i < input.length; i++) {
            for (int bit = 0; bit < 4; bit++) {
                // Shift each bit of the byte to the least significant position and mask with 1
                // to isolate the bit. The result is compared to 0 to get a boolean value.
                // Note: We start with the MSB by subtracting the bit index from 7.
                intArray[i * 4 + bit] = ((input[i] >> (3 - bit )) & 1);
                //intArray[i* 4 + bit] = bitArray[i * 4 + bit] ? 1 : 0;
            }
        }

        return intArray;
    }

    public static int[][] nibbleToBinaryBooleanArray_right(byte[] input, int rdd)
    {
        int[][] intArray = new int[input.length ][rdd]; // 4 bits per nibble

        for (int i = 0; i < input.length; i++) {
            // Process high nibble
            for (int bit = 0; bit < rdd; bit++) {
                // Extract bits of the high nibble starting from the least significant bit
                intArray[i][bit] = (input[i] >> bit) & 1;
            }
        }

        return intArray;
    }

    public static int[] binaryMatrixToDecimal(int[][] binaryMatrix) {
        int rows = binaryMatrix.length;
        int columns = binaryMatrix[0].length;
        int[] decimalValues = new int[columns];

        for (int i = 0; i < columns; i++) {
            int decimalValue = 0;
            for (int j = 0; j < rows; j++) {
                decimalValue += binaryMatrix[j][i] * Math.pow(2, j);
            }
            decimalValues[i] = decimalValue;
        }

        return decimalValues;
    }







    public static double[][] downsample(double[][] original, int q, int p) {
        int originalSamples = original[0].length; // Assuming uniform length for all rows
        int targetSamples = (int) Math.round((double) originalSamples * q / p); // Calculate target samples

        double[][] downsampledArray = new double[original.length][targetSamples];

        double interval = (double) originalSamples / targetSamples;
        for (int i = 0; i < targetSamples; i++) {
            int sampleIndex = (int) Math.round(i * interval);
            if (sampleIndex >= originalSamples) {
                sampleIndex = originalSamples - 1; // Handle edge case for rounding
            }
            for (int row = 0; row < original.length; row++) {
                downsampledArray[row][i] = original[row][sampleIndex];
            }
        }

        return downsampledArray;
    }

    public static double[] downsample(double[] original, int q, int p) {
        int originalSamples = original.length; // Assuming uniform length for all rows
        int targetSamples = (int) Math.round((double) originalSamples * q / p); // Calculate target samples

        double[] downsampledArray = new double[targetSamples];

        double interval = (double) originalSamples / targetSamples;
        for (int i = 0; i < targetSamples; i++) {
            int sampleIndex = (int) Math.round(i * interval);
            if (sampleIndex >= originalSamples) {
                sampleIndex = originalSamples - 1; // Handle edge case for rounding
            }
            downsampledArray[i] = original[sampleIndex];
        }

        return downsampledArray;
    }

    public static int getMaxIndex(double[] pks)
    {
        double maxValue = pks[0];
        int indexOfMax = 0;

        // Iterate through the array starting from the second element
        for (int i = 1; i < pks.length; i++) {
            if (pks[i] > maxValue) {
                maxValue = pks[i];
                indexOfMax = i;
            }
        }
        return indexOfMax;
    }

    public static double[] dechirp(double[][] symbol, boolean isUpChirp)
    {
        Complex[] chirp = Utils.chirp(isUpChirp,Constants.SF,Constants.BW, Constants.BW ,0,Constants.CFO,0,1);
        double[][] mod_dat = new double[2][chirp.length];
        for (int i = 0; i < chirp.length; i++){
            mod_dat[0][i] = chirp[i].getReal();
            mod_dat[1][i] = chirp[i].getImaginary();
        }
        int length = symbol[0].length;
        double[][] result = timesnative(symbol,mod_dat);

        double[][] result_spec = fftcomplexinoutnative_double(result,length);

        double[] abs_value = new double[length];
        for (int i = 0; i < abs_value.length; i++) {
            double real_1 = result_spec[0][i];
            double imaginary_1 = result_spec[1][i];
            abs_value[i] = Math.sqrt(real_1 * real_1 + imaginary_1 * imaginary_1);
        }

        return abs_value;
    }

    public static double[] dechirp_test(double[][] symbol, boolean isUpChirp, boolean issyncpreamble)
    {
        Complex[] chirp;
        if (Constants.isLinearChirp || issyncpreamble) {
            Utils.logd("dechirp linear");
            chirp = Utils.chirp(isUpChirp, Constants.SF, Constants.BW, 2 * Constants.BW, 0, Constants.CFO, 0, 1);
        } else {
            Utils.logd("dechirp nonlinear");
            chirp = Utils.getConjugate(Utils.chirpNonLinear(0, Constants.SF, Constants.NonlinearCoeff, Constants.BW, Constants.FS));
        }
        double[][] mod_dat = new double[2][chirp.length];
        for (int i = 0; i < chirp.length; i++){
            mod_dat[0][i] = chirp[i].getReal();
            mod_dat[1][i] = chirp[i].getImaginary();
        }
        int length = symbol[0].length;
        double[][] result = timesnative(symbol,mod_dat);

        double[][] result_spec = fftcomplexinoutnative_double(result,10 * length);

        double[] abs_value = new double[10 * length / 2];
        for (int i = 0; i < abs_value.length; i++) {
            double real_1 = result_spec[0][i];
            double imaginary_1 = result_spec[1][i];
            double real_2 = result_spec[0][i + abs_value.length ];
            double imaginary_2 = result_spec[1][i + abs_value.length];
            if (Constants.isLinearChirp || issyncpreamble) {
                abs_value[i] = Math.sqrt(real_1 * real_1 + imaginary_1 * imaginary_1) + Math.sqrt(real_2 * real_2 + imaginary_2 * imaginary_2);
            } else {
                abs_value[i] = Math.sqrt(real_1 * real_1 + imaginary_1 * imaginary_1);
            }
        }

        return abs_value;
    }

    public static double[][] downversion(double[] data)
    {
        // check raw data is correct => pass
        if (Constants.allowLog) {
            StringBuilder data_strBuilder = new StringBuilder();
            for (int j = 0; j < 20; j++) {
                data_strBuilder.append(data[j]);
                data_strBuilder.append(",");
            }
            String data_str = data_strBuilder.toString();
            Utils.log("raw data first 10 => " + data_str);
        }

        double[] t = new double[data.length];
        for (int i = 0; i<t.length; i++){
            t[i] = i / (double)Constants.FS ;
        }
        double[][] carrier = new double[2][data.length];
        for (int i = 0; i< t.length; i++)
        {
            carrier[0][i] = Math.cos(2* Math.PI* Constants.FC * t[i]);
            carrier[1][i] = Math.sin(2* Math.PI* Constants.FC * t[i]);
            //carrier_sin[i] = Math.sin(2* Math.PI* Constants.FC * t[i]);
        }

        double[][] downversion_chirp = new double[2][data.length];
        for (int i = 0; i< data.length; i++)
        {
            downversion_chirp[0][i] = data[i]  * carrier[0][i]; // do we really need to divded by 32767.0?
            downversion_chirp[1][i] = - data[i]  * carrier[1][i];


        }

        // check downversion_chirp is correct => Pass
        if (Constants.allowLog) {
            StringBuilder downversion_chirpBuilder = new StringBuilder();
            for (int j = 0; j < 20; j++) {
                downversion_chirpBuilder.append(downversion_chirp[0][j]);
                downversion_chirpBuilder.append(",");
                downversion_chirpBuilder.append(downversion_chirp[1][j]);
                downversion_chirpBuilder.append(",");
            }
            String downversion_chirp_str = downversion_chirpBuilder.toString();
            Utils.log("downversion_chirp first 10 => " + downversion_chirp_str);
        }

        // low-filter 4k filter
        //downversion_chirp[0] = filter(downversion_chirp[0]);
        //downversion_chirp[1] = filter(downversion_chirp[1]);
        Utils.log("length of data before bpass =>" + downversion_chirp[0].length);
        downversion_chirp[0] = bpass_filter2(downversion_chirp[0],Constants.Center_Freq,Constants.Offset_Freq,Constants.FS);
        Utils.log("length of data after bpass =>" + downversion_chirp[0].length);

        downversion_chirp[1] = bpass_filter2(downversion_chirp[1],Constants.Center_Freq,Constants.Offset_Freq,Constants.FS);

        return downversion_chirp;

    }

    public static float getBatteryLevel(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        if (batteryStatus == null)
            return -1; // Unable to retrieve battery status

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        if (level == -1 || scale == -1)
            return -1; // Unable to retrieve battery level or scale

        float batteryPct = (level / (float) scale) * 100;
        return batteryPct;
    }

    public static double[] synchronization(int up_index, int down_index)
    {
        double up_freq =(double) up_index /  Constants.Sample_Lora * Constants.FS;
        double down_freq =(double) down_index /Constants.Sample_Lora * Constants.FS;

        int up_shift = (up_index > 64) ? up_index - Constants.Sample_Lora : up_index ;
        int down_shift = (down_index > 64) ? down_index - Constants.Sample_Lora : down_index;

        double CFO = (double)(up_shift + down_shift) / 2;

        double TO = (double) (down_shift - up_shift) / 2 ;

        return new double[]{CFO, TO};
    }

    public static double[] synchronization2(double up_index, double down_index)
    {
        double compensate_index = 0.0;
        int sf = Constants.SF;
        if (sf >= 3 && sf <= 7)
        {
            compensate_index = Math.pow(2,sf - 1);
        }
        double up_shift = (up_index > compensate_index) ? up_index - Constants.Sample_Lora : up_index ;
        double down_shift = (down_index > compensate_index) ? down_index - Constants.Sample_Lora : down_index;

        double CFO = (up_shift + down_shift) / 2;

        double TO =  (down_shift - up_shift) / 2;

        return new double[]{CFO, TO};
    }

    public static int MaxIndex(double[] values){
        double maxValue = values[0];
        int indexofMax = 0;
        for (int i = 1; i< values.length; i++){
            if (values[i] > maxValue) {
                maxValue = values[i];
                indexofMax = i;
            }
        }
        return indexofMax;
    }

    public static double MaxValue(double[] values){
        double maxValue = values[0];
        for (int i = 1; i< values.length; i++){
            if (values[i] > maxValue) {
                maxValue = values[i];
            }
        }
        return maxValue;
    }

    public static double[] bPassFilter(int centerFre, int offsetFre, int sampFre) {
        int N = (int) Math.ceil(3.6 * sampFre / offsetFre);
        int M = N - 1;
        M += M % 2; // Ensure M is even

        double Wp1 = 2 * Math.PI * (centerFre - offsetFre / 2) / sampFre;
        double Wp2 = 2 * Math.PI * (centerFre + offsetFre / 2) / sampFre;

        double[] h = new double[M + 1]; // Filter coefficients

        for (int k = 0; k <= M; k++) {
            if (k - M / 2.0 == 0) {
                h[k] = (Wp2 / Math.PI) - (Wp1 / Math.PI);
            } else {
                h[k] = (Wp2 * Math.sin(Wp2 * (k - M / 2.0)) / (Math.PI * (Wp2 * (k - M / 2.0))))
                        - (Wp1 * Math.sin(Wp1 * (k - M / 2.0)) / (Math.PI * (Wp1 * (k - M / 2.0))));
            }
        }

        return h;
    }


    // written by chatgpt: simulate the bpass filter in matlab code base
    public static double[] bpass_filter2(double[] data, double centerFre, double offsetFre, double sampFre) {
        double Ap = 0.82;
        double As = 45;
        double Wp1 = 2 * Math.PI * (centerFre - offsetFre) / sampFre;
        double Wp2 = 2 * Math.PI * (centerFre + offsetFre) / sampFre;

        int N = (int) Math.ceil(3.6 * sampFre / offsetFre);
        int M = N - 1;
        M = M % 2 + M;

        // here temp is to convert matlab and java index
        double[] h = new double[M + 1];
        for (int k = 0; k <= M; k++) {
            double temp = k + 1;
            if (temp - 1 - 0.5 * M == 0) {
                h[k] = Wp2 / Math.PI - Wp1 / Math.PI;
            } else {
                h[k] = Wp2 * Math.sin(Wp2 * (temp - 1 - 0.5 * M)) / (Math.PI * (Wp2 * (temp - 1 - 0.5 * M)))
                        - Wp1 * Math.sin(Wp1 * (temp - 1 - 0.5 * M)) / (Math.PI * (Wp1 * (temp - 1 - 0.5 * M)));
            }
        }
        // print filter
        if (Constants.allowLog) {
            StringBuilder filter_strBuilder = new StringBuilder();
            for (int j = 0; j < M + 1; j++) {
                filter_strBuilder.append(h[j]);
                filter_strBuilder.append(",");
            }
            String filter_str = filter_strBuilder.toString();
            Utils.log("bpass filter => " + filter_str);
        }



        return applyFilter(h, data);
    }

    private static double[] applyFilter(double[] h, double[] data) {
        int filterLength = h.length;
        int dataLength = data.length;
        double[] result = new double[dataLength];

        for (int i = 0; i < dataLength; i++) {
            result[i] = 0;
            for (int j = 0; j < filterLength; j++) {
                if (i - j >= 0) {
                    result[i] += h[j] * data[i - j];
                }
            }
        }

        return result;
    }

    public static Complex[] chirp1(boolean isUpChirp, double T, int bw, int fs, int symbol)
    {
        int samPerSym = (int)(T * fs);
        double k, f0;
        if (isUpChirp) {
            k = bw / T;
            f0 = -bw / 2.0 ;
        } else {
            k = -bw / T;
            f0 = bw / 2.0 ;
        }

        double phi = 0;
        double[] t1 = new double[samPerSym];
        for (int i = 0; i<t1.length; i++){
            t1[i] = (i / (double)fs) ;
        }

        Complex[] c1 = new Complex[t1.length];
        for (int i = 0; i < t1.length; i++){
            double freq = f0  + 0.5 *k * t1[i];
            c1[i] = new Complex(0,2* Math.PI* freq * t1[i]).exp();
        }

        return c1;


    }

    // non-linear chirp
    public static Complex[] chirpNonLinear(int encodedData, int SF, double[] coefficientVector, double BW, double sampleRate) {
        // Initialization
        double endT = Math.pow(2, SF) / BW;
        double symbol = encodedData;
        double initFrequency = symbol / endT;

        // Coefficient Processing
        double total = Arrays.stream(coefficientVector).sum();
        // Normalization
        for (int i = 0; i < coefficientVector.length; i++) {
            coefficientVector[i] /= total;
        }
        // Degree of polynomial function
        int degree = coefficientVector.length;
        double[] divisors = new double[degree];
        for (int i = 0; i < degree; i++) {
            divisors[i] = Math.pow(endT, degree + 1 - (i + 1));
        }
        for (int i = 0; i < degree; i++) {
            coefficientVector[i] = BW * coefficientVector[i] / divisors[i];
        }
        double[] coeff = new double[degree + 1];
        System.arraycopy(coefficientVector, 0, coeff, 0, coefficientVector.length);
        coeff[coeff.length - 1] = -BW / 2;

        // From Phase to Signals
        int numSamples = (int) (endT * sampleRate);
        Complex[] y = new Complex[numSamples];

        int t1_end = (int) Math.round(Math.sqrt((BW - initFrequency) / (BW/endT/endT))*sampleRate);

        for (int i = 0; i < t1_end; i++) {
            double t = i / sampleRate;
            double[] polyint_res = polyint(coeff);
            double polyval_res = polyval(polyint_res, t);
            double phase = 2 * Math.PI * polyval_res + 2 * Math.PI * initFrequency * t;
            y[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }

        for (int i = t1_end; i < numSamples; i++) {
            double t = i / sampleRate;
            double[] polyint_res = polyint(coeff);
            double polyval_res = polyval(polyint_res, t);
            double phase = 2 * Math.PI * polyval_res + 2 * Math.PI * (-BW/2) * t;
            y[i] = new Complex(Math.cos(phase), Math.sin(phase));
        }

        return y;
    }

    private static double[] polyint(double[] coeff) {
        double[] integral = new double[coeff.length + 1];
        integral[integral.length-1] = 0; // last is 0
        for (int i = 0; i < integral.length-1; i++) { // ignore last one
            integral[i] = coeff[i] / (integral.length - i - 1);
        }
        return integral;
    }

    private static double polyval(double[] coeff, double x) {
        double result = 0;
        double len = coeff.length;
        for (int i = 0; i < coeff.length; i++) {
            result = result + Math.pow(x,len - i-1)*coeff[i];
        }
        return result;
    }

    public static Complex[] getConjugate(Complex[] array) {
        Complex[] conjugateArray = new Complex[array.length];
        for (int i = 0; i < array.length; i++) {
            conjugateArray[i] = array[i].conjugate();
        }
        return conjugateArray;
    }

    // non-linear chirp





    public static Complex[] chirp(boolean isUpChirp, int sf, int bw, int fs, double h, double cfo, double tdelta, double tscale) {
        int N = (int)Math.pow(2,sf);
        double T = N / ((double)bw /tscale);  // symbol period
        int sampPerSym = (int)Math.round(fs / (double)bw * N / tscale);
        double hOrig = h;
        h = Math.round(h);
        cfo = cfo + (hOrig - h) / N * bw;
        double k, f0;

        if (isUpChirp) {
            k = bw / T;
            f0 = -bw / 2.0 + cfo;
        } else {
            k = -bw / T;
            f0 = bw / 2.0 + cfo;
        }

        double phi = 0;
        double[] t1 = new double[(int) Math.round(sampPerSym * (N - h) / (double) N) + 1];
        for (int i = 0; i<t1.length; i++){
            t1[i] = (i / (double)fs)  + tdelta;
        }

        Complex[] c1 = new Complex[t1.length];
        for (int i = 0; i < t1.length; i++){
            double freq = f0 + k * T * h / N + 0.5 *k * t1[i];
            c1[i] = new Complex(0,2* Math.PI* freq * t1[i]).exp();
        }
        if (t1.length == 0){
            phi = 0;
        }
        else{
            phi = Math.atan2(c1[c1.length-1].getImaginary(), c1[c1.length-1].getReal());
        }
        int t2length = sampPerSym + 1 - t1.length;
        double[] t2 = new double[t2length];


        for (int i = 0; i <t2.length; i++){
            t2[i] = (i/ (double)fs) + tdelta;
        }


        Complex[] c2 = new Complex[t2.length];
        for (int i = 0; i<t2.length; i++){
            double freq = f0 + 0.5 * k * t2[i];
            c2[i] = new Complex(0,phi + 2* Math.PI * freq * t2[i] ).exp();
        }
        Complex[] partC1 = Arrays.copyOfRange(c1, 0, c1.length - 1);

        // Step 2: Concatenate partC1 and c2
        Complex[] y = new Complex[partC1.length + c2.length];
        System.arraycopy(partC1, 0, y, 0, partC1.length);
        System.arraycopy(c2, 0, y, partC1.length, c2.length);

        return y;
    }

    public static short[] GenerateEqualizationPreamble_LoRa() {
        Complex[] chirp;
        chirp = Utils.chirp(true, Constants.SF_Equalization, Constants.BW_Equalization, Constants.FS, 0, 0, 0, 1); // Assuming this method exists and returns Complex[]
        double[][] symbol = new double[2][chirp.length];

        for (int i = 0, j = 0; i < chirp.length; i++) {
            // Scale the real and imaginary parts to the maximum range of short type

            symbol[0][i] = chirp[i].getReal();
            symbol[1][i] = chirp[i].getImaginary();
        }
        double[][] results_double = timesnative(symbol,Constants.carrier_Equalization); // result[0] is cos(a)cos(b) - sin(a)sin(b) = cos(a+b)
        short[] results_real = new short[results_double[0].length];
        for (int i =0 ; i<results_real.length;i++)
        {
            results_real[i] = (short) (results_double[0][i] * 32767.0);
        }

        return results_real;
    }

    public static short[] GeneratePreamble_LoRa(boolean isUpChirp, int sym, boolean issyncpreamble)
    {
        Complex[] chirp;
        if (Constants.isLinearChirp || issyncpreamble) {
            Utils.logd("send linear");
            chirp = Utils.chirp(isUpChirp, Constants.SF, Constants.BW, Constants.FS, sym, 0, 0, 1); // Assuming this method exists and returns Complex[]
        } else {
            Utils.logd("send nonlinear");

            chirp = Utils.chirpNonLinear(sym, Constants.SF, Constants.NonlinearCoeff, Constants.BW, Constants.FS);
        }
        // Preparing to store real and imaginary components scaled and converted to short

        double[][] symbol = new double[2][chirp.length];

        for (int i = 0, j = 0; i < chirp.length; i++) {
            // Scale the real and imaginary parts to the maximum range of short type

            symbol[0][i] = chirp[i].getReal();
            symbol[1][i] = chirp[i].getImaginary();
        }
        double[][] results_double = timesnative(symbol,Constants.carrier); // result[0] is cos(a)cos(b) - sin(a)sin(b) = cos(a+b)
        short[] results_real = new short[results_double[0].length];
        for (int i =0 ; i<results_real.length;i++)
        {
            results_real[i] = (short) (results_double[0][i] * 32767.0);
        }

        return results_real;

    }

    public static short[] GenerateChirp_LoRa(boolean isUpChirp)
    {
        Complex[] chirp = Utils.chirp1(isUpChirp, 0.5,6000, Constants.FS, 0); // Assuming this method exists and returns Complex[]
        // Preparing to store real and imaginary components scaled and converted to short

        double[][] symbol = new double[2][chirp.length];

        for (int i = 0, j = 0; i < chirp.length; i++) {
            // Scale the real and imaginary parts to the maximum range of short type

            symbol[0][i] = chirp[i].getReal();
            symbol[1][i] = chirp[i].getImaginary();
        }
        double[] t = new double[chirp.length];
        for (int i = 0; i<t.length; i++){
            t[i] = i / (double)Constants.FS ;
        }
        double[][] carrier = new double[2][chirp.length];
        for (int i = 0; i< t.length; i++)
        {
            carrier[0][i] = Math.cos(2* Math.PI* 3000 * t[i]);
            carrier[1][i] = Math.sin(2* Math.PI* 3000 * t[i]);
            //carrier_sin[i] = Math.sin(2* Math.PI* Constants.FC * t[i]);
        }

        double[][] results_double = timesnative(symbol,carrier);
        short[] results_real = new short[results_double[0].length];
        for (int i =0 ; i<results_real.length;i++)
        {
            results_real[i] = (short) (results_double[0][i] * 32767.0);
        }

        return results_real;

    }

    public static byte[] Embedding2Bytes(long[] embedding) {
        StringBuilder binaryStringBuilder = new StringBuilder();

        // Step 1: Convert all long integers to a single binary string with each being a 10-bit segment
        for (long value : embedding) {
            // Mask with 0x3FF to ensure only the lowest 10 bits are used
            String binaryString = String.format("%10s", Long.toBinaryString(value & 0x3FF)).replace(' ', '0');
            binaryStringBuilder.append(binaryString);
        }

        // Step 2: Convert the binary string into a byte array
        String allBits = binaryStringBuilder.toString();
        int numBytes = (allBits.length() + 7) / 8; // Calculate how many bytes are needed
        byte[] byteList = new byte[numBytes];

        for (int i = 0, byteIndex = 0; i < allBits.length(); i += 8, byteIndex++) {
            // Ensure not to go out of bounds on the last byte
            int end = Math.min(i + 8, allBits.length());
            String byteString = allBits.substring(i, end);
            // Pad to 8 bits if necessary (on the last byte)
            byteString = String.format("%-8s", byteString).replace(' ', '0');
            byte newByte = (byte) Integer.parseInt(byteString, 2);
            byteList[byteIndex] = newByte;
        }

        return byteList;
    }

    public static long[] Bytes2Embedding(byte[] bytes) {
        // Step 1: Convert bytes to a binary string
        StringBuilder binaryStringBuilder = new StringBuilder();
        for (byte b : bytes) {
            String binaryString = String.format("%8s", Integer.toBinaryString(b & 0xFF)).replace(' ', '0');
            binaryStringBuilder.append(binaryString);
        }

        // The complete binary string
        String allBits = binaryStringBuilder.toString();
        int numInts = allBits.length() / 10; // Calculate how many 10-bit integers are needed

        long[] longs = new long[numInts];
        for (int i = 0, intIndex = 0; i + 10 <= allBits.length(); i += 10, intIndex++) {
            String intString = allBits.substring(i, i + 10);
            long newLong = Long.parseLong(intString, 2);
            longs[intIndex] = newLong;
        }

        return longs;
    }




    public static int[] concatArrays_int(int[] array1, int[] array2) {
        int[] result = new int[array1.length + array2.length];
        System.arraycopy(array1, 0, result, 0, array1.length);
        System.arraycopy(array2, 0, result, array1.length, array2.length);
        return result;
    }

    public static double[] copyArray2(Double[] sig) {
        double[] out = new double[sig.length];
        for (int i = 0; i < sig.length; i++) {
            out[i]=sig[i];
        }
        return out;
    }

    public static String pad2(String str) {
        int padlen=Constants.maxbits-1-str.length();  // minus one because of one meta bit
        String out = "";
        int counter=0;
        for (int i = 0; i < padlen; i++) {
            out+="0";
        }
        return out+str;
    }

    public static void log(String s) {
        if (Constants.allowLog) {
            Log.e(Constants.LOG, s);
            (MainActivity.av).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Constants.debugPane.getText().toString().length() > 400) {
                        Constants.debugPane.setText("");
                    }
                    Constants.debugPane.setText(Constants.debugPane.getText() + "\n" + s);
                    scrollToBottom();
                }
            });
        }
    }


    // log debug
    public static void logd(String s) {
        if (Constants.allowLog) {
            Log.d(Constants.LOG, s);
            (MainActivity.av).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (Constants.debugPane.getText().toString().length() > 400) {
                        Constants.debugPane.setText("");
                    }
                    Constants.debugPane.setText(Constants.debugPane.getText() + "\n" + s);
                    scrollToBottom();
                }
            });
        }
    }

    public static double[] sum(double[] a, double[] b) {
        double[] out = new double[a.length];
        for (int i = 0; i < a.length; i++) {
            out[i] = a[i]+b[i];
        }
        return out;
    }

    public static double[] concat(Double[] a, Double[] b) {
        double[] out = new double[a.length+b.length];
        int counter=0;
        for (int i = 0; i < a.length; i++) {
            out[counter++] = a[i];
        }
        for (int i = 0; i < b.length; i++) {
            out[counter++] = b[i];
        }
        return out;
    }

    public static double[] concat_array_list(ArrayList<Double[]> a, int n, Double[] b){
        int totalLength = b.length;
        int start = Math.max(a.size() - n, 0);
        for (int i = start; i < a.size(); i++) {
            totalLength += a.get(i).length;
        }

        // Create out array
        double[] out = new double[totalLength];

        // Copy elements from last n arrays in sampleHistory to out
        int outIndex = 0;
        for (int i = start; i < a.size(); i++) {
            for (double val : a.get(i)) {
                out[outIndex++] = val;
            }
        }

        // Concat rec to out
        for (double val : b) {
            out[outIndex++] = val;
        }

        return out;
    }

    public static double[] concat3(Double[] a, Double[] b, Double[] c) {
        double[] out = new double[a.length+b.length+c.length];
        int counter=0;
        for (int i = 0; i < a.length; i++) {
            out[counter++] = a[i];
        }
        for (int i = 0; i < b.length; i++) {
            out[counter++] = b[i];
        }
        for (int i = 0; i < c.length; i++) {
            out[counter++] = c[i];
        }
        return out;
    }

    public static double[] concat4(Double[] a, Double[] b, Double[] c, Double[] d) {
        double[] out = new double[a.length+b.length+c.length+d.length];
        int counter=0;
        for (int i = 0; i < a.length; i++) {
            out[counter++] = a[i];
        }
        for (int i = 0; i < b.length; i++) {
            out[counter++] = b[i];
        }
        for (int i = 0; i < c.length; i++) {
            out[counter++] = c[i];
        }
        for (int i = 0; i < d.length; i++) {
            out[counter++] = d[i];
        }
        return out;
    }

    public static double[] concat(double[] a, double[] b) {
        double[] out = new double[a.length+b.length];
        int counter=0;
        for (int i = 0; i < a.length; i++) {
            out[counter++] = a[i];
        }
        for (int i = 0; i < b.length; i++) {
            out[counter++] = b[i];
        }
        return out;
    }



    public static short[] concat_short(short[] a, short[] b) {
        short[] out = new short[a.length+b.length];
        int counter=0;
        for (int i = 0; i < a.length; i++) {
            out[counter++] = a[i];
        }
        for (int i = 0; i < b.length; i++) {
            out[counter++] = b[i];
        }
        return out;
    }

    public static double sum(double[] a) {
        double val = 0;
        for (int i = 0; i < a.length; i++) {
            val += a[i];
        }
        return val;
    }

    public static double[] pow(double[] sig, int pow) {
        double[] out = new double[sig.length];
        for (int i = 0; i < sig.length; i++) {
            out[i] = Math.pow(sig[i],pow);
        }
        return out;
    }

    public static void scrollToBottom() {
        Constants.sview.post(new Runnable() {
            public void run() {
                Constants.sview.smoothScrollTo(0, Constants.debugPane.getBottom());
            }
        });
    }

    public static String trim(String s) {
        return s.substring(1,s.length()-1);
    }

    public static String trim_end(String s) {
        if (s.charAt(s.length()-2) == ',') {
            return s.substring(0, s.length() - 2);
        }
        return s;
    }

    public static short[] random_array(int N) {
        Random random = new Random(1);
        short[] out = new short[N];
        for (int i = 0; i < N; i++) {
            out[i] = (short)random.nextInt(2);
        }
        return out;
    }

    public static int LevenshteinDistance(String s0, String s1) {
        int len0 = s0.length() + 1;
        int len1 = s1.length() + 1;

        // the array of distances
        int[] cost = new int[len0];
        int[] newcost = new int[len0];

        // initial cost of skipping prefix in String s0
        for (int i = 0; i < len0; i++)
            cost[i] = i;

        // dynamicaly computing the array of distances

        // transformation cost for each letter in s1
        for (int j = 1; j < len1; j++) {

            // initial cost of skipping prefix in String s1
            newcost[0] = j - 1;

            // transformation cost for each letter in s0
            for (int i = 1; i < len0; i++) {

                // matching current letters in both strings
                int match = (s0.charAt(i - 1) == s1.charAt(j - 1)) ? 0 : 1;

                // computing cost for each transformation
                int cost_replace = cost[i - 1] + match;
                int cost_insert = cost[i] + 1;
                int cost_delete = newcost[i - 1] + 1;

                // keep minimum cost
                newcost[i] = Math.min(Math.min(cost_insert, cost_delete),
                        cost_replace);
            }

            // swap cost/newcost arrays
            int[] swap = cost;
            cost = newcost;
            newcost = swap;
        }

        // the distance is the cost for transforming all letters in both strings
        return cost[len0 - 1];
    }

    public static double[] convert(short[] s) {
        double[] out = new double[s.length];
        for (int i = 0; i < s.length; i++) {
            out[i] = s[i];
        }
        return out;
    }

    public static short[] convert(String s) {
        short[] out = new short[s.length()];
        int counter=0;
        for (int i = 0; i < s.length(); i++) {
            out[counter++]=Short.parseShort(""+s.charAt(i));
        }
        return out;
    }

    public static Double[] convert2(short[] s) {
        Double[] out = new Double[s.length];
        for (int i = 0; i < s.length; i++) {
            out[i] = (double)s[i];
        }
        return out;
    }

    public static int[] convert(double[] s) {
        int[] out = new int[s.length];
        for (int i = 0; i < s.length; i++) {
            out[i] = (int)s[i];
        }
        return out;
    }

    public static short[] convert_s(double[] s) {
        short[] out = new short[s.length];
        for (int i = 0; i < s.length; i++) {
            out[i] = (short)s[i];
        }
        return out;
    }

    public static String genName(Constants.SignalType sigType, int m_attempt) {
//        return Constants.user.toString()+"-"+sigType.toString()+"-"+m_attempt+"-"+Constants.ts;
        return Constants.user.toString()+"-"+sigType.toString()+"-"+m_attempt;
    }

    public static String genName(Constants.SignalType sigType, int m_attempt, int chirpLoopNumber) {
//        return Constants.user.toString()+"-"+sigType.toString()+"-"+m_attempt+"-"+Constants.ts;
        return Constants.user.toString()+"-"+sigType.toString()+"-"+m_attempt+"-"+chirpLoopNumber;
    }

    public static String trimmed_ts() {
        String ts = Constants.ts+"";
        return ts.substring(ts.length()-4,ts.length());
    }

    public static double min(double[] sig, int idx1, int idx2) {
        double min = 100000;
        for (int i = idx1; i < idx2; i++) {
            if (sig[i] < min) {
                min = sig[i];
            }
        }
        return min;
    }

    public static double max(double[] sig, int idx1, int idx2) {
        double max = -100000;
        for (int i = idx1; i < idx2; i++) {
            if (sig[i] > max) {
                max = sig[i];
            }
        }
        return max;
    }

    public static double mean(double[] sig, int idx1, int idx2) {
        double val = 0;
        int counter=0;
        for (int i = idx1; i < idx2; i++) {
            val += sig[i];
            counter++;
        }
        return val/counter;
    }

    public static double mean(double[] sig) {
        double val = 0;
        int counter=0;
        for (int i = 0; i < sig.length; i++) {
            val += sig[i];
            counter++;
        }
        return val/counter;
    }

    public static double var(double[] sig) {
        double val=0;
        double mu = mean(sig);
        for (int i = 0; i < sig.length; i++) {
            val += Math.pow(sig[i]-mu,2);
        }
        return val/sig.length;
    }

    public static double[] max(double[] sig) {
        double max = -100000;
        int max_index = -1;
        for (int i = 0; i < sig.length; i++) {
            if (sig[i] > max) {
                max = sig[i];
                max_index = i;
            }
        }
        double[] max_info = new double[2];
        max_info[0] = max;
        max_info[1] = max_index;
        return max_info;
    }

    public static int[] max(short[] sig) {
        int max = -32767;
        int max_index = -1;
        for (int i = 0; i < sig.length; i++) {
            if (sig[i] > max) {
                max = sig[i];
                max_index = i;
            }
        }
        int[] max_info = new int[2];
        max_info[0] = max;
        max_info[1] = max_index;
        return max_info;
    }

    public static short[] abs(short[] sig) {
        short[] out = new short[sig.length];
        for (int i = 0; i < sig.length; i++) {
            out[i] = (short)Math.abs(sig[i]);
        }
        return out;
    }

    public static int[] convert(List<Integer> sig) {
        int[] out = new int[sig.size()];
        int counter = 0;
        for (Integer i : sig) {
            out[counter++]=i;
        }
        return out;
    }

    public static double[] add(double[] a, double[] b) {
        double[] out = new double[a.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = a[i]+b[i];
        }
        return out;
    }

    public static double[] subtract(double[] a, double[] b) {
        double[] out = new double[a.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = a[i]-b[i];
        }
        return out;
    }

    public static double[] subtract(double[] a, double b) {
        double[] out = new double[a.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = a[i]-b;
        }
        return out;
    }

    public static double mag2db(double data) {
        return 20*Math.log10(data);
    }

    public static double[] mag2db(double[] data) {
        double[] out = new double[data.length];
        for (int i = 0; i < out.length; i++) {
            out[i] = 20*Math.log10(data[i]);
        }
        return out;
    }

    public static double pow2db(double data) {
        return 10*Math.log10(data);
    }

    public static double[] segment(double[] data, int i, int j) {
        double[] out = new double[j-i+1];
        int counter=0;
        for (int k = i; k <= j; k++) {
            out[counter++] = data[k];
        }
        return out;
    }



    public static int[] segment(int[] data, int i, int j) {
        int[] out = new int[j-i+1];
        int counter=0;
        for (int k = i; k <= j; k++) {
            out[counter++] = data[k];
        }
        return out;
    }

    public static double[][] segment2(double[][] data, int i, int j) {
        double[][] out = new double[2][j-i+1];
        int counter=0;
        for (int k = i; k <= j; k++) {
            out[0][counter] = data[0][k];
            out[1][counter] = data[1][k];
            counter += 1;
        }
        return out;
    }

    public static short[] segment(short[] data, int i, int j) {
        short[] out = new short[j-i+1];
        int counter=0;
        for (int k = i; k <= j; k++) {
            out[counter++] = data[k];
        }
        return out;
    }

    public static int[] freqs2bins(int[] freqs) {
        int[] bins = new int[freqs.length];
        int freqSpacing = Constants.fs/Constants.Ns;
        for (int i = 0; i < freqs.length; i++) {
            bins[i] = freqs[i]/freqSpacing;
        }
        return bins;
    }

    public static int[] bins2freqs(int[] bins) {
        int[] freqs = new int[bins.length];
        int freqSpacing = Constants.fs/Constants.Ns;
        for (int i = 0; i < bins.length; i++) {
            freqs[i] = bins[i] * freqSpacing;
        }
        return freqs;
    }

    public static double[][] segment(double[][] data, int i, int j) {
        double[][] out = new double[data.length][j-1+1];
        for (int a = 0; a < data.length; a++) {
            double[] outi = new double[j - i + 1];
            int counter = 0;
            for (int k = i; k < j; k++) {
                out[counter++] = data[k];
            }
            out[a]=outi;
        }
        return out;
    }

    public static int xcorr(double[] preamble, double[] filt, double[] sig, int sig_len, Constants.SignalType sigType) {
        if (Constants.xcorr_method==1) {
            return xcorr_m1(preamble,filt,sig,sig_len);
        }
        else if (Constants.xcorr_method==2) {
            return xcorr_m2(preamble,filt,sig,sig_len, sigType);
        }
        return -1;
    }

    public static int xcorr_m1(double[] preamble, double[] filt, double[] sig, int sig_len) {
        int seglen=48000;
        int moveamount=24000;

        int numsegs = (filt.length/moveamount)-1;
        int counter = 0;

        int maxidx=0;
        double maxval=0;
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < numsegs; i++) {
            Log.e("xcorr",counter+","+(counter+seglen-1));
            double[] seg = Utils.segment(filt,counter,counter+seglen-1);
            double[] corr = xcorr_helper(preamble,seg);
//            String aa=Arrays.toString(seg);
//            String bb=Arrays.toString(preamble);

            int max_idx = (int)max_idx(corr)[0];
            int xcorr_idx = (transform_idx(max_idx, seg.length));
            if (corr[max_idx] > maxval) {
                if ((i == numsegs-1 && xcorr_idx > seg.length-preamble.length) ||
                        (xcorr_idx > seg.length-preamble.length)) {
                    //pass
                }
                else {
                    maxval = corr[max_idx];
                    maxidx = xcorr_idx + counter;
                }
            }
            counter=counter+moveamount;
            Log.e(LOG, ">>"+counter+","+seglen+","+seg.length+","+(counter+xcorr_idx)+","+corr[max_idx]);
        }
        Log.e(LOG, ">>"+maxidx);
        Log.e("xcorr","xcorr runtime "+(System.currentTimeMillis()-t1));
        return maxidx;
    }

    public static double[] xcorr_online(double[] preamble, double[] filt) {
        double[] corr = xcorr_helper(preamble,filt);
        return evalSegv2(filt, corr);
    }

    public static double[] calculateSNR_null(double[] spec_symbol) {
        double[] snrs = new double[Constants.subcarrier_number_default];
        int bin_size=2;
        int scounter=0;
        for (Integer bin : Constants.valid_carrier_preamble) {
            double signal = Utils.pow2db(Math.pow(spec_symbol[bin],2));
            double[] seg1 = Utils.segment(spec_symbol,bin-(bin_size+1),bin-2);
            double[] seg2 = Utils.segment(spec_symbol,bin+2,bin+(bin_size+1));
            double sum_val=0;
            double counter=0;
            for (Double d : seg1) {
                sum_val+=Math.pow(d,2);
                counter++;
            }
            for (Double d : seg2) {
                sum_val+=Math.pow(d,2);
                counter++;
            }
            sum_val /= counter;

            double noise = Utils.pow2db(sum_val);

            double snr = signal-noise;
            snrs[scounter++]=(int)snr;
        }

//        Log.e("asdf","SNRS: "+snrs);

        return snrs;
    }

    public static boolean isLegit(double[] sig, Constants.SignalType sigType, double[] preamble, int start) {
        if (sigType.equals(Constants.SignalType.Sounding)) {
            int start_idx = start + preamble.length + Constants.ChirpGap;
            int end_idx = start_idx + (Constants.Ns * Constants.chanest_symreps) - 1;
            if (end_idx <= sig.length - 1) {
                double[] rx_symbols = Utils.segment(sig, start_idx, end_idx);
                double[] spec = (Utils.fftnative_double(rx_symbols,rx_symbols.length));
                double[] snrs = calculateSNR_null(spec);
                Log.e("cands_snr",Utils.mean(snrs)+"");

                FileOperations.appendtofile(MainActivity.av, Utils.mean(snrs)+"\n",
                        Utils.genName(Constants.SignalType.ASymCheckSNR, 0)+".txt");

                if (Utils.mean(snrs) > Constants.CheckSymSNRThresh) {
                    return true;
                }
            }
        } else if (sigType.equals(Constants.SignalType.Feedback)) {
            int start_idx = start + preamble.length + Constants.ChirpGap;
            int end_idx = start_idx + (int)(Constants.fs*(fbackTime/1000.0)) - 1;
            if (end_idx <= sig.length - 1) {
                double[] cand_sig = Utils.segment(sig, start_idx, end_idx);
                double[] spec = Utils.mag2db(Utils.fftnative_double(cand_sig, cand_sig.length));
                int[] freqs = FeedbackSignal.decodeFeedbackSignal(spec);
                Log.e("cands_fre", freqs[0] + "," + freqs[1]);

                FileOperations.appendtofile(MainActivity.av, freqs[0]+","+freqs[1]+"\n",
                        Utils.genName(Constants.SignalType.ASymCheckFreq, 0)+".txt");

                if (freqs[0] == -1) {
                    return false;
                }
                return true;
            }
        }
        return false;
    }

    public static double[] evalSegv2(double[] filt, double[] corr) {
        int[] cands=Utils.getCandidateLocs(corr);

        double max=0;
        int maxidx=0;
        for (int j = 0; j < cands.length; j++) {
            int idx = (transform_idx(cands[j], filt.length));
            double[] legit2 = Naiser.Naiser_check_valid(filt, idx);
            if (legit2[1]>max) {
                max=legit2[1];
                maxidx = idx;
            }
            if (legit2[0] > 0) {
                return new double[]{corr[cands[j]], idx, legit2[1]};
            }
        }
        return new double[]{-1,maxidx,max};
    }

    public static double[] evalSegv1(double[] filt, double[] corr, int counter, double[] preamble) {

        double[] maxs=max_idx(corr);
        int max_idx = (int)maxs[0];
        double max_val = maxs[1];
        int xcorr_idx = (transform_idx(max_idx, filt.length));

        double[] cand_pre = Utils.segment(
                filt,xcorr_idx+counter,xcorr_idx+counter+preamble.length-1);

        double[] spec = Utils.mag2db(Utils.fftnative_double(cand_pre,cand_pre.length));
        double vv = Utils.var(Utils.segment(spec, 170, 180));
        int win=2400;
        int numAbove = Utils.numAbove(corr,max_idx-win,
                max_idx+win,max_val* XCORR_MAX_VAL_HEIGHT_FAC);
        Log.e("xcorr",String.format("%d %d %.2f %.2e %.2f %d",0,numAbove,XCORR_MAX_VAL_HEIGHT_FAC,corr[max_idx],vv, xcorr_idx+counter));

        String out = String.format("%.2e %d %.2f %.2f",corr[max_idx],numAbove,XCORR_MAX_VAL_HEIGHT_FAC,vv);

        if (corr[max_idx] > Constants.MinXcorrVal &&
                numAbove < Constants.XcorrAboveThresh && numAbove > 0
                && vv < Constants.VAR_THRESH
        ) {
            Log.e("xcorr",String.format("DETECTED %d,%.2f,%d,%d,%.1e %.2f %d",numAbove,XCORR_MAX_VAL_HEIGHT_FAC,(xcorr_idx+counter),0,corr[max_idx],vv,xcorr_idx+counter));
            FileOperations.appendtofile(MainActivity.av, "***"+out+"\n",
                    Utils.genName(Constants.SignalType.AXcorr, 0)+".txt");
            return new double[]{corr[max_idx],xcorr_idx};
        }
        else {
            FileOperations.appendtofile(MainActivity.av, out+"\n",
                    Utils.genName(Constants.SignalType.AXcorr, 0)+".txt");
            return new double[]{-1,-1};
        }
    }

    public static int numAbove(double[] sig, int idx1, int idx2, double thresh) {
        int num = 0;
        if (idx1 < 0) {
            idx1 = 0;
        }
        if (idx2 >= sig.length) {
            idx2 = sig.length-1;
        }
        for (int i = idx1; i < idx2; i++) {
            if (sig[i] > thresh) {
                num++;
            }
        }
        return num;
    }

    public static short[] flip(short[] bits) {
        short[] flipped = new short[bits.length];
        for (int i = 0; i < bits.length; i++) {
            if (bits[i] == 0) {
                flipped[i] = 1;
            }
        }
        return flipped;
    }

    public static double[][] flip2(double[][] bits) {
        double[][] flipped = new double[bits.length][bits[0].length];
        for (int i = 0; i < bits.length; i++) {
            for (int j = 0; j < bits[0].length; j++) {
                if (bits[i][j] == 1) {
                    flipped[i][j] = -1;
                }
                if (bits[i][j] == -1) {
                    flipped[i][j] = 1;
                }
            }
        }
        return flipped;
    }
    public static double[] waitForChirp(Constants.SignalType sigType, int m_attempt, int chirpLoopNumber, String TaskID) {
        String filename = Utils.genName(sigType, m_attempt, chirpLoopNumber);
        Log.e("fifo",filename);

        Constants._OfflineRecorder = new OfflineRecorder(
                MainActivity.av, Constants.fs, filename);
        Constants._OfflineRecorder.start2();

        int MAX_WINDOWS = 0;

        int numWindowsLeft = 0;
        double timeout = 0;
        int len = 0;
        int ChirpSamples = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        if (sigType.equals(Constants.SignalType.Sounding)) {
            if (Constants.Ns==960||Constants.Ns==1920) {
                MAX_WINDOWS = 2;
            }
            else if (Constants.Ns==4800) {
                MAX_WINDOWS=3;
            }
            else if (Constants.Ns==9600) {
                MAX_WINDOWS=3;
            }
            timeout = 30;
            len = ChirpSamples+Constants.ChirpGap+((Constants.Ns+Constants.Cp)*Constants.chanest_symreps);
        }
        else if (sigType.equals(Constants.SignalType.Feedback)) {
            MAX_WINDOWS = 2;
            if (Constants.Ns==960||Constants.Ns==1920) {
                timeout = 5;
            }
            else if (Constants.Ns==4800) {
                timeout=7;
            }
            else if (Constants.Ns==9600) {
                timeout=7;
            }
            len = (int)(ChirpSamples+Constants.ChirpGap+((fbackTime/1000.0)*Constants.fs));
        }

        int N = (int)(timeout*(Constants.fs/Constants.RecorderStepSize));
        double[] tx_preamble = PreambleGen.preamble_d();
        ArrayList<Double[]> sampleHistory = new ArrayList<>();
        ArrayList<Double> valueHistory = new ArrayList<>();
        ArrayList<Double> idxHistory = new ArrayList<>();
        int synclag = 12000;
        double[] sounding_signal = new double[]{};
        sounding_signal=new double[(MAX_WINDOWS+1)*Constants.RecorderStepSize];
        Log.e("len","sig length "+sounding_signal.length+","+sigType.toString());
        boolean valid_signal = false;
//        boolean getOneMoreFlag = false;
        int sounding_signal_counter=0;
        for (int i = 0; i < N; i++) {
            Double[] rec = Utils.convert2(Constants._OfflineRecorder.get_FIFO(String.valueOf(i), TaskID));
//            Log.e("timer1",m_attempt+","+rec.length+","+i+","+N+","+(System.currentTimeMillis()-t1)+"");

            if (sigType.equals(Constants.SignalType.Sounding)||
                    sigType.equals(Constants.SignalType.Feedback)||
                    sigType.equals(Constants.SignalType.DataRx) ) {
//                Log.e("fifo","loop "+i);

                if (i<MAX_WINDOWS) {
                    sampleHistory.add(rec);
                    continue;
                }
                else {
                    if (numWindowsLeft==0) {
                        double[] out = null;
                        out = Utils.concat(sampleHistory.get(sampleHistory.size() - 1), rec);

                        double[] filt = Utils.copyArray(out);
                        filt = Utils.filter(filt);

                        //value,idx
                        double[] xcorr_out = Utils.xcorr_online(tx_preamble, filt);

                        long t1 = System.currentTimeMillis();
                        Utils.log(String.format("Listening... (%.2f)",xcorr_out[2]));

                        sampleHistory.add(rec);
                        valueHistory.add(xcorr_out[0]);
                        idxHistory.add(xcorr_out[1]);

                        if (xcorr_out[0] != -1) {
                            if (xcorr_out[1] + len + synclag > Constants.RecorderStepSize*MAX_WINDOWS) {
                                Log.e("copy","one more flag "+xcorr_out[1]+","+(xcorr_out[1] + len + synclag));

                                numWindowsLeft = MAX_WINDOWS-1;

//                                Log.e("copy","copying "+out[t_idx]+","+out[t_idx+1]+","+out[t_idx+2]+","+out[t_idx+3]+","+out[t_idx+4]);
                                for (int j = (int)xcorr_out[1]; j < out.length; j++) {
                                    sounding_signal[sounding_signal_counter++]=out[j];
                                }

                                Log.e("copy", "copy ("+xcorr_out[1]+","+filt.length+") to ("+sounding_signal_counter+")");
                            } else {
                                Log.e("copy","good! "+filt.length+","+xcorr_out[1]+","+filt.length);
//                                Utils.log("good");
                                int counter=0;
                                for (int k = (int) xcorr_out[1]; k < out.length; k++) {
                                    sounding_signal[counter++] = out[k];
                                }
//                                sounding_signal = Utils.segment(filt, (int) xcorr_out[1], filt.length - 1);
                                valid_signal = true;
                                break;
                            }
                        }
                    }
                    else if (sounding_signal_counter>0){
//                        Utils.log("another window");
                        Log.e("copy","another window from "+sounding_signal_counter+","+(sounding_signal_counter+rec.length)+","+sounding_signal.length);

//                        double[] filt2 = Utils.copyArray2(rec);
//                        filt2 = Utils.filter(filt2);

                        for (int j = 0; j < rec.length; j++) {
                            sounding_signal[sounding_signal_counter++]=rec[j];
                        }
                        numWindowsLeft -= 1;
                        if (numWindowsLeft==0){
                            valid_signal=true;
                            break;
                        }
                    }

                    if(sampleHistory.size() >= 6){
                        sampleHistory.remove(0);
                        valueHistory.remove(0);
                        idxHistory.remove(0);
                    }
                }
            }
        }

        Constants._OfflineRecorder.halt2();

        if (valid_signal) {
            return Utils.filter(sounding_signal);
        }
        return null;
    }


    public static double[] waitForChirp_with_timeout(Constants.SignalType sigType, int m_attempt, int chirpLoopNumber, String TaskID) {
        String filename = Utils.genName(sigType, m_attempt, chirpLoopNumber);
        Log.e("fifo",filename);

        Constants._OfflineRecorder = new OfflineRecorder(
                MainActivity.av, Constants.fs, filename);
        Constants._OfflineRecorder.start2();

        int MAX_WINDOWS = 0;

        int numWindowsLeft = 0;
        double timeout = 0;
        int len = 0;
        int ChirpSamples = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        if (sigType.equals(Constants.SignalType.Sounding)) {
            if (Constants.Ns==960||Constants.Ns==1920) {
                MAX_WINDOWS = 2;
            }
            else if (Constants.Ns==4800) {
                MAX_WINDOWS=3;
            }
            else if (Constants.Ns==9600) {
                MAX_WINDOWS=3;
            }
            timeout = 30;
            len = ChirpSamples+Constants.ChirpGap+((Constants.Ns+Constants.Cp)*Constants.chanest_symreps);
        }
        else if (sigType.equals(Constants.SignalType.Feedback)) {
            MAX_WINDOWS = 2;
            if (Constants.Ns==960||Constants.Ns==1920) {
                timeout = 5;
            }
            else if (Constants.Ns==4800) {
                timeout=7;
            }
            else if (Constants.Ns==9600) {
                timeout=7;
            }
            len = (int)(ChirpSamples+Constants.ChirpGap+((fbackTime/1000.0)*Constants.fs));
        }

        int N = (int)(timeout*(Constants.fs/Constants.RecorderStepSize));
        double[] tx_preamble = PreambleGen.preamble_d();
        ArrayList<Double[]> sampleHistory = new ArrayList<>();
        ArrayList<Double> valueHistory = new ArrayList<>();
        ArrayList<Double> idxHistory = new ArrayList<>();
        int synclag = 12000;
        double[] sounding_signal = new double[]{};
        sounding_signal=new double[(MAX_WINDOWS+1)*Constants.RecorderStepSize];
        Log.e("len","sig length "+sounding_signal.length+","+sigType.toString());
        boolean valid_signal = false;
//        boolean getOneMoreFlag = false;
        int sounding_signal_counter=0;
        int window_count = 0;
        while (!Utils.check_pass_timeout()) {
            Double[] rec = Utils.convert2(Constants._OfflineRecorder.get_FIFO(String.valueOf(window_count), TaskID));
//            Log.e("timer1",m_attempt+","+rec.length+","+i+","+N+","+(System.currentTimeMillis()-t1)+"");

            if (sigType.equals(Constants.SignalType.Sounding)||
                    sigType.equals(Constants.SignalType.Feedback)||
                    sigType.equals(Constants.SignalType.DataRx) ) {
//                Log.e("fifo","loop "+i);

                if (window_count<MAX_WINDOWS) {
                    sampleHistory.add(rec);
//                    continue;
                }
                else {
                    if (numWindowsLeft==0) {
                        double[] out = null;
                        out = Utils.concat(sampleHistory.get(sampleHistory.size() - 1), rec);

                        double[] filt = Utils.copyArray(out);
                        filt = Utils.filter(filt);

                        //value,idx
                        double[] xcorr_out = Utils.xcorr_online(tx_preamble, filt);

                        long t1 = System.currentTimeMillis();
//                        Utils.log(String.format("Listening... (%.2f)",xcorr_out[2]));
                        Utils.log(String.format("Listening........ (%.2f, %d)",xcorr_out[2],window_count));

                        sampleHistory.add(rec);
                        valueHistory.add(xcorr_out[0]);
                        idxHistory.add(xcorr_out[1]);

                        if (xcorr_out[0] != -1) {
                            if (xcorr_out[1] + len + synclag > Constants.RecorderStepSize*MAX_WINDOWS) {
                                Log.e("copy","one more flag "+xcorr_out[1]+","+(xcorr_out[1] + len + synclag));

                                numWindowsLeft = MAX_WINDOWS-1;

//                                Log.e("copy","copying "+out[t_idx]+","+out[t_idx+1]+","+out[t_idx+2]+","+out[t_idx+3]+","+out[t_idx+4]);
                                for (int j = (int)xcorr_out[1]; j < out.length; j++) {
                                    sounding_signal[sounding_signal_counter++]=out[j];
                                }

                                Log.e("copy", "copy ("+xcorr_out[1]+","+filt.length+") to ("+sounding_signal_counter+")");
                            } else {
                                Log.e("copy","good! "+filt.length+","+xcorr_out[1]+","+filt.length);
//                                Utils.log("good");
                                int counter=0;
                                for (int k = (int) xcorr_out[1]; k < out.length; k++) {
                                    sounding_signal[counter++] = out[k];
                                }
//                                sounding_signal = Utils.segment(filt, (int) xcorr_out[1], filt.length - 1);
                                valid_signal = true;
                                break;
                            }
                        }
                    }
                    else if (sounding_signal_counter>0){
//                        Utils.log("another window");
                        Log.e("copy","another window from "+sounding_signal_counter+","+(sounding_signal_counter+rec.length)+","+sounding_signal.length);

//                        double[] filt2 = Utils.copyArray2(rec);
//                        filt2 = Utils.filter(filt2);

                        for (int j = 0; j < rec.length; j++) {
                            sounding_signal[sounding_signal_counter++]=rec[j];
                        }
                        numWindowsLeft -= 1;
                        if (numWindowsLeft==0){
                            valid_signal=true;
                            break;
                        }
                    }

                    if(sampleHistory.size() >= 6){
                        sampleHistory.remove(0);
                        valueHistory.remove(0);
                        idxHistory.remove(0);
                    }

                }
                window_count = window_count + 1;

            }
        }

        Constants._OfflineRecorder.halt2();

        if (valid_signal) {
            return Utils.filter(sounding_signal);
        }
        return null;
    }

    public static void listen_to_noise(Constants.SignalType sigType, int m_attempt, int chirpLoopNumber, String TaskID) {

        String filename = Utils.genName(sigType, m_attempt, chirpLoopNumber);
        Constants._OfflineRecorder = new OfflineRecorder(
                MainActivity.av, Constants.fs, filename);
        Constants._OfflineRecorder.start2();

        int MAX_WINDOWS = 3;

        int timeout = 30;


        double[] sounding_signal = new double[]{};
        sounding_signal = new double[MAX_WINDOWS * Constants.RecorderStepSize];
        int sounding_signal_counter = 0;
        for (int i = 0; i < timeout; i++) {
            Double[] rec = Utils.convert2(Constants._OfflineRecorder.get_FIFO(String.valueOf(i), TaskID));
            if (i < MAX_WINDOWS) {
                for (int j = 0; j < rec.length; j++) {
                    sounding_signal[sounding_signal_counter++] = rec[j];
                }
            }
        }
        Constants._OfflineRecorder.halt2();

        if (Constants.allowLog) {
            StringBuilder noiseBuilder = new StringBuilder();
            for (int j = 0; j < sounding_signal.length; j++) {
                noiseBuilder.append(sounding_signal[j]);
                noiseBuilder.append(",");
            }
            String raw_noise_signal = noiseBuilder.toString();
            if (raw_noise_signal.endsWith(",")) {
                raw_noise_signal = raw_noise_signal.substring(0, raw_noise_signal.length() - 1);
            }

            FileOperations.writetofile(MainActivity.av, raw_noise_signal + "",
                    filename + ".txt");
        }
    }

//    shared by three protocols
    public static double[] waitForData(Constants.SignalType sigType, int m_attempt, int chirpLoopNumber, String TaskID) {
        String filename = Utils.genName(sigType, m_attempt, chirpLoopNumber);
        Utils.logd(filename);

        Constants._OfflineRecorder = new OfflineRecorder(
                MainActivity.av, Constants.fs, filename);
        Constants._OfflineRecorder.start2();

        int MAX_WINDOWS = 0;

        int numWindowsLeft = 0;
        double timeout = 0;
        int len = 0;
        int ChirpSamples = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        long startTime_receive_signal = 0;
        if (sigType.equals(Constants.SignalType.DataRx)) {
            MAX_WINDOWS = 2; //
            if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt || Constants.scheme == Constants.Modulation.OFDM_freq_all)
            {
                if (Constants.IsCountingFish || Constants.IsDectectingFish)
                {
                    timeout = 3;
                    len = ChirpSamples+Constants.ChirpGap+((Constants.Ns+Constants.Cp)*32);

                }
                else if(Constants.ImagingFish)
                {
                    timeout = 15;
//                    len = ChirpSamples+Constants.ChirpGap+((Constants.Ns+Constants.Cp)*200); // TODO this can be more precise freq all and freq adapt will not have the same
                    len = ChirpSamples+Constants.ChirpGap+((Constants.Ns+Constants.Cp)*300); // TODO this can be more precise freq all and freq adapt will not have the same

                }
            }
            else if(Constants.scheme == Constants.Modulation.LoRa)
            {
                timeout = 15;
                if (Constants.IsCountingFish || Constants.IsDectectingFish)
                {
                    len = ChirpSamples+Constants.ChirpGap+Constants.Ns_lora*32;

                }
                else if(Constants.ImagingFish)
                {
                    if (Constants.SF == 7)
                    {
                        timeout = 120;
                        len = ChirpSamples+Constants.ChirpGap+(Constants.Ns_lora+ Constants.Gap)*220;
                    }
                    else if (Constants.SF == 5 || Constants.SF == 6)
                    {
                        timeout = 20;
                        len = ChirpSamples + Constants.ChirpGap +(Constants.Ns_lora+ Constants.Gap) * 450; // TODO: hardcoded for inserting preamble, make sure there is enough space when we insert preamble every 3 symbols
                    }
                    else if (Constants.SF == 4)
                    {
                        timeout = 120;
                        len = ChirpSamples+Constants.ChirpGap+(Constants.Ns_lora+ Constants.Gap)*450;
                    }
                    else if (Constants.SF == 3)
                    {
                        timeout = 120;
                        len = ChirpSamples+Constants.ChirpGap+(Constants.Ns_lora+ Constants.Gap)*600;
                    }
                }
            }
        }
        else if (sigType.equals(Constants.SignalType.DataChirp))// just collect chirp for channel estimation
        {
            MAX_WINDOWS = 2; //
            timeout = 15;
            len = ChirpSamples+Constants.ChirpGap+Constants.fs;
        }

        int N = (int)(timeout*(Constants.fs/Constants.RecorderStepSize));
        double[] tx_preamble = PreambleGen.preamble_d();

        ArrayList<Double[]> sampleHistory = new ArrayList<>();
        ArrayList<Double> valueHistory = new ArrayList<>();
        ArrayList<Double> idxHistory = new ArrayList<>();
        int synclag = 12000;
        double[] sounding_signal = new double[]{}; // sounding_signal should be larger than the transmitted signal

        if(Constants.ImagingFish)
        {
            if (sigType.equals(Constants.SignalType.DataRx))
            {
                if (Constants.scheme == Constants.Modulation.LoRa)
                {
                    if (Constants.SF == 7)
                    {
                        sounding_signal=new double[35*Constants.RecorderStepSize];
                    }
                    else if (Constants.SF == 6)
                    {
                        sounding_signal=new double[25*Constants.RecorderStepSize];
                    }
                    else if (Constants.SF == 5)
                    {
                        sounding_signal=new double[25*Constants.RecorderStepSize];
                    }
                    else if (Constants.SF == 4 ||Constants.SF == 3 )
                    {
                        sounding_signal = new double[10 * Constants.RecorderStepSize];
                    }

                }
                else if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt || Constants.scheme == Constants.Modulation.OFDM_freq_all)
                {
//                    sounding_signal=new double[15*Constants.RecorderStepSize];
                    sounding_signal=new double[25*Constants.RecorderStepSize];

                }
            }
            else if (sigType.equals(Constants.SignalType.DataChirp))
            {
                sounding_signal=new double[(MAX_WINDOWS+1)*Constants.RecorderStepSize];
            }
        }
        else {
            sounding_signal=new double[(MAX_WINDOWS+1)*Constants.RecorderStepSize];
        }

        Utils.log("sig length "+sounding_signal.length+","+sigType.toString());
        boolean valid_signal = false;
//        boolean getOneMoreFlag = false;
        int sounding_signal_counter=0;
//        for (int i = 0; i < N; i++) { // actually here we can replace it with while true
        int window_count = 0;

        // reset sensor
        Utils.reset_sensor();

        while (true) {
            Double[] rec = Utils.convert2(Constants._OfflineRecorder.get_FIFO(String.valueOf(window_count), TaskID));
//            Log.e("timer1",m_attempt+","+rec.length+","+i+","+N+","+(System.currentTimeMillis()-t1)+"");
//            Log.e("received signal ", "rec " + rec);
            if (sigType.equals(Constants.SignalType.Sounding)||
                    sigType.equals(Constants.SignalType.Feedback)||
                    sigType.equals(Constants.SignalType.DataRx) || sigType.equals(Constants.SignalType.DataChirp)) {
//                Log.e("fifo","loop "+i);

                if (window_count<MAX_WINDOWS) {
                    sampleHistory.add(rec);
//                    continue;
                }
                else {
                    if (numWindowsLeft==0) { // do xcorr, signal finder
                        double[] out = null; // will be reset, so out's size will not explode
                        out = Utils.concat(sampleHistory.get(sampleHistory.size() - 1 ), rec);
                        //out = Utils.concat_array_list(sampleHistory,MAX_WINDOWS-1,rec);

                        double[] filt = Utils.copyArray(out);
                        filt = Utils.filter(filt);

                        //value,idx
                        double[] xcorr_out = Utils.xcorr_online(tx_preamble, filt);
                        if (sigType.equals(Constants.SignalType.DataRx)){
                            Utils.log("xcorr_out" + xcorr_out[0] + ' ' +  xcorr_out[1]);
                        }

                        long t1 = System.currentTimeMillis();
                        Utils.log(String.format("Listening........ (%.2f, %d)",xcorr_out[2],window_count));

                        sampleHistory.add(rec);
                        valueHistory.add(xcorr_out[0]);
                        idxHistory.add(xcorr_out[1]);

                        if (xcorr_out[0] != -1) {
                            // init the Receiver_Latency_Str all three protocols
                            Constants.Receiver_Latency_Str = "";
                            String formattedNow = "";
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                LocalDateTime now = LocalDateTime.now();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                                formattedNow = now.format(formatter);
                            } else {
                                formattedNow = "not_available";
                            }
                            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + formattedNow + "\n" + Constants.scheme.toString() + "\n";
                            // receiver t1 - receive signal
                            startTime_receive_signal = SystemClock.elapsedRealtime();

                            if (xcorr_out[1] + len + synclag > Constants.RecorderStepSize*MAX_WINDOWS) {
                                Log.e("copy","one more flag "+xcorr_out[1]+","+(xcorr_out[1] + len + synclag));
                                //Utils.log("need more windows" + xcorr_out[1] + len + synclag + ' ' +  Constants.RecorderStepSize*MAX_WINDOWS);
                                if (sigType.equals(Constants.SignalType.DataChirp))
                                {
                                    numWindowsLeft = MAX_WINDOWS - 1;
                                }
                                else
                                {
                                    numWindowsLeft = (int) (xcorr_out[1] + len + synclag - Constants.RecorderStepSize*MAX_WINDOWS) / Constants.RecorderStepSize + 1;
                                }

                                Utils.log("need more windows" + xcorr_out[1] + ' ' + len + ' ' + synclag + ' ' +  Constants.RecorderStepSize*MAX_WINDOWS + ' ' + numWindowsLeft);

//                                Log.e("copy","copying "+out[t_idx]+","+out[t_idx+1]+","+out[t_idx+2]+","+out[t_idx+3]+","+out[t_idx+4]);
                                for (int j = (int)xcorr_out[1]; j < out.length; j++) {
                                    sounding_signal[sounding_signal_counter++]=out[j];
                                }

                                // plot Spec and SNR
//                                ChannelEstimate.extractSignal_withsymbol_helper(MainActivity.av, sounding_signal, 0, m_attempt);

                                Utils.logd("copy ("+xcorr_out[1]+","+filt.length+") to ("+sounding_signal_counter+")");
                            }
                            else {
                                Utils.logd("good! "+filt.length+","+xcorr_out[1]+","+filt.length);
                                Utils.log("good");
                                int counter=0;
                                for (int k = (int) xcorr_out[1]; k < out.length; k++) {
                                    sounding_signal[counter++] = out[k];
                                }
//                                sounding_signal = Utils.segment(filt, (int) xcorr_out[1], filt.length - 1);
                                valid_signal = true;
                                break;
                            }
                        }
                    }
                    else if (sounding_signal_counter>0){
//                        Utils.log("another window");
                        Utils.logd("another window from "+sounding_signal_counter+","+(sounding_signal_counter+rec.length)+","+sounding_signal.length);

//                        double[] filt2 = Utils.copyArray2(rec);
//                        filt2 = Utils.filter(filt2);

                        for (int j = 0; j < rec.length; j++) {
                            sounding_signal[sounding_signal_counter++]=rec[j];
                        }
                        numWindowsLeft -= 1;
                        if (numWindowsLeft==0){
                            valid_signal=true;
                            break;
                        }
                    }

                    if(sampleHistory.size() >= 6){ // here make sure it will not explode
                        sampleHistory.remove(0);
                        valueHistory.remove(0);
                        idxHistory.remove(0);
                    }
                }
                window_count = window_count + 1;

            }
        }

        Constants._OfflineRecorder.halt2();

        // write sensor
        Utils.stop_sensor();
        FileOperations.writeSensors(MainActivity.av, Utils.genName(Constants.SignalType.Receiver_Sensor, 0) + ".txt");


        if (valid_signal) {
            //return Utils.filter(sounding_signal);
            // receiver t1 - receive signal
            final long inferenceTime_receive_signal = SystemClock.elapsedRealtime() - startTime_receive_signal;
            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver receive signal (ms): " + inferenceTime_receive_signal + "\n";
            Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);


            return sounding_signal;

        }
        return null;
    }

    public static double[] waitForData_with_timeout(Constants.SignalType sigType, int m_attempt, int chirpLoopNumber, String TaskID) {
        String filename = Utils.genName(sigType, m_attempt, chirpLoopNumber);
        Utils.logd(filename);

        Constants._OfflineRecorder = new OfflineRecorder(
                MainActivity.av, Constants.fs, filename);
        Constants._OfflineRecorder.start2();

        int MAX_WINDOWS = 0;

        int numWindowsLeft = 0;
        double timeout = 0;
        int len = 0;
        int ChirpSamples = (int)((Constants.preambleTime/1000.0)*Constants.fs);
        long startTime_receive_signal = 0;
        if (sigType.equals(Constants.SignalType.DataRx)) {
            MAX_WINDOWS = 2; //
            if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt || Constants.scheme == Constants.Modulation.OFDM_freq_all)
            {
                if (Constants.IsCountingFish || Constants.IsDectectingFish)
                {
                    timeout = 3;
                    len = ChirpSamples+Constants.ChirpGap+((Constants.Ns+Constants.Cp)*32);

                }
                else if(Constants.ImagingFish)
                {
                    timeout = 15;
//                    len = ChirpSamples+Constants.ChirpGap+((Constants.Ns+Constants.Cp)*200); // TODO this can be more precise freq all and freq adapt will not have the same
                    len = ChirpSamples+Constants.ChirpGap+((Constants.Ns+Constants.Cp)*300); // TODO this can be more precise freq all and freq adapt will not have the same

                }
            }
            else if(Constants.scheme == Constants.Modulation.LoRa)
            {
                timeout = 15;
                if (Constants.IsCountingFish || Constants.IsDectectingFish)
                {
                    len = ChirpSamples+Constants.ChirpGap+Constants.Ns_lora*32;

                }
                else if(Constants.ImagingFish)
                {
                    if (Constants.SF == 7)
                    {
                        timeout = 120;
                        len = ChirpSamples+Constants.ChirpGap+(Constants.Ns_lora+ Constants.Gap)*220;
                    }
                    else if (Constants.SF == 5 || Constants.SF == 6)
                    {
                        timeout = 20;
                        len = ChirpSamples + Constants.ChirpGap +(Constants.Ns_lora+ Constants.Gap) * 450; // TODO: hardcoded for inserting preamble, make sure there is enough space when we insert preamble every 3 symbols
                    }
                    else if (Constants.SF == 4)
                    {
                        timeout = 120;
                        len = ChirpSamples+Constants.ChirpGap+(Constants.Ns_lora+ Constants.Gap)*450;
                    }
                    else if (Constants.SF == 3)
                    {
                        timeout = 120;
                        len = ChirpSamples+Constants.ChirpGap+(Constants.Ns_lora+ Constants.Gap)*600;
                    }
                }
            }
        }
        else if (sigType.equals(Constants.SignalType.DataChirp))// just collect chirp for channel estimation
        {
            MAX_WINDOWS = 2; //
            timeout = 15;
            len = ChirpSamples+Constants.ChirpGap+Constants.fs;
        }

        int N = (int)(timeout*(Constants.fs/Constants.RecorderStepSize));
        double[] tx_preamble = PreambleGen.preamble_d();

        ArrayList<Double[]> sampleHistory = new ArrayList<>();
        ArrayList<Double> valueHistory = new ArrayList<>();
        ArrayList<Double> idxHistory = new ArrayList<>();
        int synclag = 12000;
        double[] sounding_signal = new double[]{}; // sounding_signal should be larger than the transmitted signal

        if(Constants.ImagingFish)
        {
            if (sigType.equals(Constants.SignalType.DataRx))
            {
                if (Constants.scheme == Constants.Modulation.LoRa)
                {
                    if (Constants.SF == 7)
                    {
                        sounding_signal=new double[35*Constants.RecorderStepSize];
                    }
                    else if (Constants.SF == 6)
                    {
                        sounding_signal=new double[25*Constants.RecorderStepSize];
                    }
                    else if (Constants.SF == 5)
                    {
                        sounding_signal=new double[25*Constants.RecorderStepSize];
                    }
                    else if (Constants.SF == 4 ||Constants.SF == 3 )
                    {
                        sounding_signal = new double[10 * Constants.RecorderStepSize];
                    }

                }
                else if (Constants.scheme == Constants.Modulation.OFDM_freq_adapt || Constants.scheme == Constants.Modulation.OFDM_freq_all)
                {
//                    sounding_signal=new double[15*Constants.RecorderStepSize];
                    sounding_signal=new double[25*Constants.RecorderStepSize];

                }
            }
            else if (sigType.equals(Constants.SignalType.DataChirp))
            {
                sounding_signal=new double[(MAX_WINDOWS+1)*Constants.RecorderStepSize];
            }
        }
        else {
            sounding_signal=new double[(MAX_WINDOWS+1)*Constants.RecorderStepSize];
        }

        Utils.log("sig length "+sounding_signal.length+","+sigType.toString());
        boolean valid_signal = false;
//        boolean getOneMoreFlag = false;
        int sounding_signal_counter=0;
//        for (int i = 0; i < N; i++) { // actually here we can replace it with while true
        int window_count = 0;
        // start sensor
        Utils.reset_sensor();
        while (!Utils.check_pass_timeout()) {
            Double[] rec = Utils.convert2(Constants._OfflineRecorder.get_FIFO(String.valueOf(window_count), TaskID));
//            Log.e("timer1",m_attempt+","+rec.length+","+i+","+N+","+(System.currentTimeMillis()-t1)+"");
//            Log.e("received signal ", "rec " + rec);
            if (sigType.equals(Constants.SignalType.Sounding)||
                    sigType.equals(Constants.SignalType.Feedback)||
                    sigType.equals(Constants.SignalType.DataRx) || sigType.equals(Constants.SignalType.DataChirp)) {
//                Log.e("fifo","loop "+i);

                if (window_count<MAX_WINDOWS) {
                    sampleHistory.add(rec);
//                    continue;
                }
                else {
                    if (numWindowsLeft==0) { // do xcorr, signal finder
                        double[] out = null; // will be reset, so out's size will not explode
                        out = Utils.concat(sampleHistory.get(sampleHistory.size() - 1 ), rec);
                        //out = Utils.concat_array_list(sampleHistory,MAX_WINDOWS-1,rec);

                        double[] filt = Utils.copyArray(out);
                        filt = Utils.filter(filt);

                        //value,idx
                        double[] xcorr_out = Utils.xcorr_online(tx_preamble, filt);
                        if (sigType.equals(Constants.SignalType.DataRx)){
                            Utils.log("xcorr_out" + xcorr_out[0] + ' ' +  xcorr_out[1]);
                        }

                        long t1 = System.currentTimeMillis();
                        Utils.log(String.format("Listening........ (%.2f, %d)",xcorr_out[2],window_count));

                        sampleHistory.add(rec);
                        valueHistory.add(xcorr_out[0]);
                        idxHistory.add(xcorr_out[1]);

                        if (xcorr_out[0] != -1) {
                            // init the Receiver_Latency_Str all three protocols
                            Constants.Receiver_Latency_Str = "";
                            String formattedNow = "";
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                LocalDateTime now = LocalDateTime.now();
                                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
                                formattedNow = now.format(formatter);
                            } else {
                                formattedNow = "not_available";
                            }
                            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + formattedNow + "\n" + Constants.scheme.toString() + "\n";
                            // receiver t1 - receive signal
                            startTime_receive_signal = SystemClock.elapsedRealtime();

                            if (xcorr_out[1] + len + synclag > Constants.RecorderStepSize*MAX_WINDOWS) {
                                Log.e("copy","one more flag "+xcorr_out[1]+","+(xcorr_out[1] + len + synclag));
                                //Utils.log("need more windows" + xcorr_out[1] + len + synclag + ' ' +  Constants.RecorderStepSize*MAX_WINDOWS);
                                if (sigType.equals(Constants.SignalType.DataChirp))
                                {
                                    numWindowsLeft = MAX_WINDOWS - 1;
                                }
                                else
                                {
                                    numWindowsLeft = (int) (xcorr_out[1] + len + synclag - Constants.RecorderStepSize*MAX_WINDOWS) / Constants.RecorderStepSize + 1;
                                }

                                Utils.log("need more windows" + xcorr_out[1] + ' ' + len + ' ' + synclag + ' ' +  Constants.RecorderStepSize*MAX_WINDOWS + ' ' + numWindowsLeft);

//                                Log.e("copy","copying "+out[t_idx]+","+out[t_idx+1]+","+out[t_idx+2]+","+out[t_idx+3]+","+out[t_idx+4]);
                                for (int j = (int)xcorr_out[1]; j < out.length; j++) {
                                    sounding_signal[sounding_signal_counter++]=out[j];
                                }

                                // plot Spec and SNR
//                                ChannelEstimate.extractSignal_withsymbol_helper(MainActivity.av, sounding_signal, 0, m_attempt);

                                Utils.logd("copy ("+xcorr_out[1]+","+filt.length+") to ("+sounding_signal_counter+")");
                            }
                            else {
                                Utils.logd("good! "+filt.length+","+xcorr_out[1]+","+filt.length);
                                Utils.log("good");
                                int counter=0;
                                for (int k = (int) xcorr_out[1]; k < out.length; k++) {
                                    sounding_signal[counter++] = out[k];
                                }
//                                sounding_signal = Utils.segment(filt, (int) xcorr_out[1], filt.length - 1);
                                valid_signal = true;
                                break;
                            }
                        }
                    }
                    else if (sounding_signal_counter>0){
//                        Utils.log("another window");
                        Utils.logd("another window from "+sounding_signal_counter+","+(sounding_signal_counter+rec.length)+","+sounding_signal.length);

//                        double[] filt2 = Utils.copyArray2(rec);
//                        filt2 = Utils.filter(filt2);

                        for (int j = 0; j < rec.length; j++) {
                            sounding_signal[sounding_signal_counter++]=rec[j];
                        }
                        numWindowsLeft -= 1;
                        if (numWindowsLeft==0){
                            valid_signal=true;
                            break;
                        }
                    }

                    if(sampleHistory.size() >= 6){ // here make sure it will not explode
                        sampleHistory.remove(0);
                        valueHistory.remove(0);
                        idxHistory.remove(0);
                    }
                }
                window_count = window_count + 1;

            }
        }
        if (Utils.check_pass_timeout()) {
            Utils.logd("Pass time out waiting data " + Constants.scheme.name() + ": " + Constants.datacollection_current_instance_index + " " + Constants.datacollection_time_out_map[Constants.datacollection_current_instance_index]);
        }
        Constants._OfflineRecorder.halt2();

        // write sensor
        Utils.stop_sensor();
        FileOperations.writeSensors(MainActivity.av, Utils.genName(Constants.SignalType.Receiver_Sensor, 0) + ".txt");

        if (valid_signal) {
            //return Utils.filter(sounding_signal);
            // receiver t1 - receive signal
            final long inferenceTime_receive_signal = SystemClock.elapsedRealtime() - startTime_receive_signal;
            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver receive signal (ms): " + inferenceTime_receive_signal + "\n";
            Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);


            return sounding_signal;

        }
        return null;
    }

    public static int xcorr_m2(double[] preamble, double[] filt, double[] sig, int sig_len, Constants.SignalType sigType) {
        int seglen=48000;
        int moveamount=24000;

        int numsegs = (filt.length/moveamount)-1;
        int counter = 0;

        int maxidx=-1;
        long t1 = System.currentTimeMillis();
        for (int i = 0; i < numsegs; i++) {
            double[] seg = Utils.segment(filt,counter,counter+seglen-1);
            double[] corr = xcorr_helper(preamble,seg);

            double[] maxs=max_idx(corr);
            int max_idx = (int)maxs[0];
            int xcorr_idx = (transform_idx(max_idx, seg.length));
            Log.e(">>",xcorr_idx+"");

            if ((i == numsegs-1 && xcorr_idx > seg.length-preamble.length) ||
                    (xcorr_idx > seg.length-preamble.length)) {
                Log.e("","");
            }
            else {
                double[] status = evalSegv2(filt, corr);
                Log.e(">>",status[0]+","+status[1]);
                if (status[0] != -1) {
                    maxidx = xcorr_idx + counter;
                }
            }
            counter=counter+moveamount;
        }

        return maxidx;
    }

    public static int[] getCandidateLocs(double[] corr) {
        double[] corr2 = Utils.copyArray(corr);
        int MAX_CANDS = 3;
        int[] maxvals=new int[MAX_CANDS];
        for (int i = 0; i < MAX_CANDS; i++) {
            double[] maxs=Utils.max(corr2);
            maxvals[i] = (int)maxs[1];
            int idx1 = Math.max(0,(int)maxs[1]-2400);
            int idx2 = Math.min(corr.length-1,(int)maxs[1]+2400);
            for (int j = idx1; j < idx2; j++) {
                corr2[j]=0;
            }
        }
        return maxvals;
    }

    public static double[] xcorr_helper(double[] preamble, double[] sig) {
        double[][] a = Utils.fftcomplexoutnative_double(preamble, sig.length);
        double[][] b = Utils.fftcomplexoutnative_double(sig, sig.length);
        Utils.conjnative(b);
        double[][] multout = Utils.timesnative(a, b);
        double[] corr = Utils.ifftnative(multout);
        return corr;
    }

    public static double[] xcorr_matlab_abs(double[] x, double[] y) {
        int lenX = x.length;
        int lenY = y.length;
        int maxLen = Math.max(lenX, lenY);

        // Pad the shorter array with zeros
        double[] xPadded = new double[maxLen];
        double[] yPadded = new double[maxLen];
        System.arraycopy(x, 0, xPadded, 0, lenX);
        System.arraycopy(y, 0, yPadded, 0, lenY);

        // Compute cross-correlation
        double[] r = new double[2 * maxLen - 1];
        for (int lag = -maxLen + 1; lag < maxLen; lag++) {
            double sum = 0;
            for (int i = 0; i < maxLen; i++) {
                int j = i + lag;
                if (j >= 0 && j < maxLen) {
                    sum += xPadded[i] * yPadded[j];
                }
            }
            r[maxLen - 1 - lag] = Math.abs(sum);
        }

        return r;
    }

    public static double[] max_idx(double[] corr) {
        double maxval=0;
        int maxidx=0;
        for (int i = 0; i < corr.length; i++) {
            if (corr[i]>maxval){
                maxval=corr[i];
                maxidx=i;
            }
        }
        return new double[]{maxidx,maxval};
    }

    public static int transform_idx(int maxidx, int sig_len) {
        return sig_len-(maxidx*2)-1;
    }

    public static double[] abs(double[][] data) {
        double[] out = new double[data[0].length];
        for (int i = 0; i < data[0].length; i++) {
            out[i]=Math.sqrt((data[0][i] * data[0][i]) + (data[1][i] * data[1][i]));
        }
        return out;
    }
    public static double[] filter(double[] sig) {
        return firwrapper(sig);
    }

    public static double[] firwrapper(double[] sig) {
        double[] h=new double[]{0.000182981959336989,0.000281596242979397,0.000278146045925432,0.000175593510303356,-4.62343304809110e-19,-0.000200360419689133,-0.000360951066953434,-0.000412991328953827,-0.000300653514269367,1.50969613210241e-18,0.000465978441137468,0.00102258147190892,0.00155366635073282,0.00192784355834049,0.00203610307379658,0.00183119111593167,0.00135603855040258,0.000749280342023432,0.000220958680427305,-2.30998316092680e-19,0.000264751350126403,0.00107567949517942,0.00233229627684471,0.00377262258405226,0.00502311871694995,0.00569227410155340,0.00548603174290836,0.00431270688583350,0.00234309825736876,0,-0.00213082326080853,-0.00345315860003227,-0.00353962427138790,-0.00228740687362881,3.04497082814396e-18,0.00264042059820580,0.00471756289619728,0.00531631859929438,0.00379212348265709,-1.99178361782373e-17,-0.00558925290045386,-0.0119350370250123,-0.0176440086545525,-0.0213191620435004,-0.0219592383672450,-0.0193021014035209,-0.0140079988312124,-0.00761010753938763,-0.00221480214028708,-3.05997847316821e-18,-0.00261988600696407,-0.0106615919949044,-0.0233019683686087,-0.0382766989959210,-0.0522058307502582,-0.0612344107673335,-0.0618649720104803,-0.0518011604210193,-0.0306049219949348,2.05563094310381e-17,0.0362729247397242,0.0730450660051671,0.104645449260725,0.125975754818137,0.133506082359307,0.125975754818137,0.104645449260725,0.0730450660051671,0.0362729247397242,2.05563094310381e-17,-0.0306049219949348,-0.0518011604210193,-0.0618649720104803,-0.0612344107673335,-0.0522058307502582,-0.0382766989959210,-0.0233019683686087,-0.0106615919949044,-0.00261988600696407,-3.05997847316821e-18,-0.00221480214028708,-0.00761010753938763,-0.0140079988312124,-0.0193021014035209,-0.0219592383672450,-0.0213191620435004,-0.0176440086545525,-0.0119350370250123,-0.00558925290045386,-1.99178361782373e-17,0.00379212348265709,0.00531631859929438,0.00471756289619728,0.00264042059820580,3.04497082814396e-18,-0.00228740687362881,-0.00353962427138790,-0.00345315860003227,-0.00213082326080853,0,0.00234309825736876,0.00431270688583350,0.00548603174290836,0.00569227410155340,0.00502311871694995,0.00377262258405226,0.00233229627684471,0.00107567949517942,0.000264751350126403,-2.30998316092680e-19,0.000220958680427305,0.000749280342023432,0.00135603855040258,0.00183119111593167,0.00203610307379658,0.00192784355834049,0.00155366635073282,0.00102258147190892,0.000465978441137468,1.50969613210241e-18,-0.000300653514269367,-0.000412991328953827,-0.000360951066953434,-0.000200360419689133,-4.62343304809110e-19,0.000175593510303356,0.000278146045925432,0.000281596242979397,0.000182981959336989};
        double[] filtout = fir(sig,h);
        return Utils.segment(filtout,63,filtout.length-1);
    }

    public static double[] bpass_filter(double[] sig, int center_freq, int offset_freq, int sampling_freq)
    {
        double[] h = bPassFilter(center_freq,offset_freq,sampling_freq);
        double[] filtout = fir(sig,h);
        return Utils.segment(filtout,63,filtout.length-1);
    }

    public static int[] arange(int i, int j) {
        int[] out = new int[j-i+1];
        for (int k = 0; k < out.length; k++) {
            out[k]=i+k;
        }
        return out;
    }

    public static double[] copyArray(double[] sig) {
        double[] out = new double[sig.length];
        for (int i = 0; i < sig.length; i++) {
            out[i]=sig[i];
        }
        return out;
    }

    public static double[] div(double[] sig, double val) {
        double[] out = new double[sig.length];
        for (int i = 0; i < sig.length; i++) {
            out[i] = sig[i]/val;
        }
        return out;
    }
    public static int[] linspace(int start, int inc, int end) {
        int num=(end-start)/inc;
        int[] out = new int[num];
        for (int i = 0; i < num; i++) {
            out[i] = start+(inc*i);
        }
        return out;
    }

    public static String getDirName() {
        Constants.currentDirPath = Constants.exp_num+"_"+Constants.ts;
        return Constants.currentDirPath;
    }

    public static String getImageLevelDirPath(String imagename) {
        return getDirName() + "/" + imagename;
    }

    public static String getMethodLevelDirPath(String imagename, String method) {
        return getImageLevelDirPath(imagename) + "/" + method;
    }

    public static String getSetupLevelDirPath(String imagename, String method, String setup_description) {
        return getMethodLevelDirPath(imagename, method) + "/" + setup_description;
    }

    public static String getExpInstanceLevelDirPath(String imagename, String method, String setup_description, int exp_id) {
        return getSetupLevelDirPath(imagename, method, setup_description) + "/" + exp_id;
    }

    public static void update_setup_description() {
        Constants.setup_description = Constants.datacollection_env + "_" + Constants.datacollection_distance + "_" + Constants.datacollection_mobility + "_" + Constants.datacollection_depth + "_" + Constants.datacollection_orientation + "_" + Constants.gap_from_spinner;
    }

    public static int check_in_used_datacollection_scheme(String method) {
        for (String s : Constants.all_datacollection_schemes) {
            if (s.equals(method)) {
                return 1;
            }
        }
        return 0;
    }
    public static void update_estimated_time() {
        Constants.estimated_time_in_second = Constants.datacollection_init_delay_time + Constants.datacollection_times * Constants.datacollection_image_count * (Constants.datacollection_proposed_time * check_in_used_datacollection_scheme("proposed") +Constants.datacollection_css_time * check_in_used_datacollection_scheme("css") + Constants.datacollection_ofdm_adapt_time * check_in_used_datacollection_scheme("ofdm_adapt") + Constants.datacollection_ofdm_wo_adapt_time * check_in_used_datacollection_scheme("ofdm_wo_adapt")) + (Constants.all_datacollection_schemes.length * Constants.datacollection_image_count * Constants.datacollection_mode_switch_time);
    }

    public static String convertSecondsToTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return minutes + " Min " + seconds + " Seconds";
    }

    public static boolean isInteger(String s) {
        try {
            Integer.parseInt(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }
    public static boolean isFloat(String s) {
        try {
            Float.parseFloat(s);
        } catch(NumberFormatException e) {
            return false;
        } catch(NullPointerException e) {
            return false;
        }
        // only got here if we didn't return false
        return true;
    }

    public static double sum_multiple(double[] a, double[] b) {
        double out = 0;
        for (int i = 0; i < a.length; i++) {
            out += a[i]*b[i];
        }
        return out;
    }

    public static void sendNotification(Context mContext, String title, String message, int icon) {
        SharedPreferences prefs = mContext.getSharedPreferences("prefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mContext.getSharedPreferences("prefs", Context.MODE_PRIVATE).edit();
//        if (AppPreferencesHelper.areNotificationsEnabled(mContext, Constants.NOTIFS_ENABLED)) {
            Log.e("notif", "notif");
            NotificationManager mNotificationManager;

            NotificationCompat.Builder mBuilder =
                    new NotificationCompat.Builder(mContext.getApplicationContext(), "channel");
            Intent ii = new Intent(mContext.getApplicationContext(), MainActivity.class);
            ii.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext, 0, ii, PendingIntent.FLAG_IMMUTABLE);

            NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle();
            bigText.bigText(message);
            bigText.setBigContentTitle(title);
//            bigText.setSummaryText(message);

            mBuilder.setContentIntent(pendingIntent);
            mBuilder.setSmallIcon(icon);
            mBuilder.setContentTitle(title);
            mBuilder.setContentText(message);
            mBuilder.setPriority(Notification.PRIORITY_MAX);
            mBuilder.setStyle(bigText);
//        mBuilder.setBadgeIconType(R.drawable.logo2);

            mNotificationManager =
                    (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

// === Removed some obsoletes
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                String channelId = "Your_channel_id";
                NotificationChannel channel = new NotificationChannel(
                        channelId,
                        "Channel human readable title",
                        NotificationManager.IMPORTANCE_HIGH);
                mNotificationManager.createNotificationChannel(channel);
                channel.setShowBadge(true);
                mBuilder.setChannelId(channelId);
            }

            int notifID = prefs.getInt("notif_id_pkey", 0);
            mNotificationManager.notify(notifID, mBuilder.build());
            editor.putInt("notif_id_pkey", notifID + 1);
            editor.commit();
//        }
    }

    public static void checkTextInput(int[] range, EditText textinput, String ss) {
        try {
            int ss_int = Integer.parseInt(ss);
            if (!Arrays.stream(range).anyMatch(i -> i == ss_int)) {
                textinput.setError("Input must be in " + Arrays.toString(range));
                return;
            }
        } catch (Exception e) {
            textinput.setError("Input must be integer and not empty");
        }
    }

    public static void checkTextInput(float[] range, EditText textinput, String ss) {
        try {
            float ss_float = Float.parseFloat(ss);
            boolean found = false;
            for (float value : range) {
                if (value == ss_float) {
                    found = true;
                    break;
                }
            }
            if (found == false) {
                textinput.setError("Input must be in " + Arrays.toString(range));
                return;
            }
        } catch (Exception e) {
            textinput.setError("Input must be float and not empty");
        }
    }

    public static String assetFilePath(Context context, String assetName) throws IOException {
        File file = new File(context.getFilesDir(), assetName);
        if (file.exists() && file.length() > 0) {
            return file.getAbsolutePath();
        } else {
            throw new FileNotFoundException("The file " + assetName + " does not exist or is empty in the directory " + context.getFilesDir().getAbsolutePath());
        }
    }


    public static long[] encode_image(Bitmap mBitmap) {
        float[] mu = {0.0f, 0.0f, 0.0f};
        float[] std = {1.0f, 1.0f, 1.0f};
        final Tensor tempInputTensor = TensorImageUtils.bitmapToFloat32Tensor(mBitmap,
                mu, std);
        // Convert the PyTorch tensor to a float array
        float[] tempArray = tempInputTensor.getDataAsFloatArray();
        // Apply the operation x = 2 * x - 1 to the float array
        for (int i = 0; i < tempArray.length; i++) {
            tempArray[i] = 2.0f * tempArray[i] - 1.0f;
        }
        // Convert the float array back to a PyTorch tensor
        Tensor inputTensor = Tensor.fromBlob(tempArray, tempInputTensor.shape()); // Adjust the shape as needed
        final long startTime = SystemClock.elapsedRealtime();
        // run model
        Tensor outTensors = Constants.mEncoder1.forward(IValue.from(inputTensor)).toTensor();
        outTensors = Constants.mEncoder2.forward(IValue.from(outTensors)).toTensor();
        outTensors = Constants.mEncoder3.forward(IValue.from(outTensors)).toTensor();
        final long[] results = outTensors.getDataAsLongArray();

        Constants.encode_sequence = results;

        return results;
    }

    public static Bitmap decode_image(long[] results) {
        Tensor inputTensordecode = Tensor.fromBlob(results, new long[]{64});
        Tensor outTensorsdecode = Constants.mDecoder1.forward(IValue.from(inputTensordecode)).toTensor();
        outTensorsdecode = Constants.mDecoder2.forward(IValue.from(outTensorsdecode)).toTensor();
        outTensorsdecode = Constants.mDecoder3.forward(IValue.from(outTensorsdecode)).toTensor();
        final byte[] rgbData = outTensorsdecode.getDataAsUnsignedByteArray();
        int[] argbPixels = new int[Constants.compressImageSize * Constants.compressImageSize]; // Array to hold ARGB pixel data.
        int pixelIndex = 0;
        int argbIndex = 0;
        for (int y = 0; y < Constants.compressImageSize; y++) {
            for (int x = 0; x < Constants.compressImageSize; x++) {
                int r = rgbData[pixelIndex++] & 0xFF; // Red component
                int g = rgbData[pixelIndex++] & 0xFF; // Green component
                int b = rgbData[pixelIndex++] & 0xFF; // Blue component
                int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
                argbPixels[argbIndex++] = argb; // Store the ARGB value in the array.
            }
        }
        Bitmap vqganGtBitmap = Bitmap.createBitmap(argbPixels, Constants.compressImageSize, Constants.compressImageSize, Bitmap.Config.ARGB_8888);
        return vqganGtBitmap;
    }

    public static void decode_image_receiver(long[] results, ImageView mImageView, boolean before) {
        // receiver t6 decode image 1 (before recover)
        final long startTime_decode_image = SystemClock.elapsedRealtime();
        Tensor inputTensordecode = Tensor.fromBlob(results, new long[]{64});
        Tensor outTensorsdecode = Constants.mDecoder1.forward(IValue.from(inputTensordecode)).toTensor();
        outTensorsdecode = Constants.mDecoder2.forward(IValue.from(outTensorsdecode)).toTensor();
        outTensorsdecode = Constants.mDecoder3.forward(IValue.from(outTensorsdecode)).toTensor();
        final byte[] rgbData = outTensorsdecode.getDataAsUnsignedByteArray();
        int[] argbPixels = new int[Constants.compressImageSize * Constants.compressImageSize]; // Array to hold ARGB pixel data.
        int pixelIndex = 0;
        int argbIndex = 0;
        for (int y = 0; y < Constants.compressImageSize; y++) {
            for (int x = 0; x < Constants.compressImageSize; x++) {
                int r = rgbData[pixelIndex++] & 0xFF; // Red component
                int g = rgbData[pixelIndex++] & 0xFF; // Green component
                int b = rgbData[pixelIndex++] & 0xFF; // Blue component
                int argb = 0xFF000000 | (r << 16) | (g << 8) | b;
                argbPixels[argbIndex++] = argb; // Store the ARGB value in the array.
            }
        }
        Bitmap vqganDecodedBitmap = Bitmap.createBitmap(argbPixels, Constants.compressImageSize, Constants.compressImageSize, Bitmap.Config.ARGB_8888);
        // receiver t6 decode image 1 (before recover)
        final long inferenceTime_decode_image = SystemClock.elapsedRealtime() - startTime_decode_image;
        if (before == true) {
            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver decode image (before recover) (ms): " + inferenceTime_decode_image + "\n";
        } else if (before == false) {
            Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver decode image (after recover) (ms): " + inferenceTime_decode_image + "\n";

        }
        Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

        mImageView.post(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(vqganDecodedBitmap);
            }
        });

        // save receive image before recover
        if (Constants.allowLog) {
            if (before == true) {
                FileOperations.saveBitmapToFile(MainActivity.av, vqganDecodedBitmap, Utils.genName(Constants.SignalType.Received_Bitmap, 0) + ".png");
            } else if (before == false) {
                FileOperations.saveBitmapToFile(MainActivity.av, vqganDecodedBitmap, Utils.genName(Constants.SignalType.Recovered_Bitmap, 0) + ".png");

            }
        }
    }



    public static long[] transformer_recover(long[] embeddings) {

        // receiver t5 transformer recover
        final long startTime_transformer_recover = SystemClock.elapsedRealtime();

        long[] prediction = new long[embeddings.length];

        Tensor inputTensorTransformer = Tensor.fromBlob(embeddings, new long[]{1, 64});
        Tensor inputTensorTransformer2 = Tensor.fromBlob(embeddings, new long[]{1, 64});

        Utils.log("shape: " + Arrays.toString(inputTensorTransformer.shape()));
        final long startTimeTransformer = SystemClock.elapsedRealtime();
        for (int p = 0; p < Constants.recover_round; p++) {
            IValue result = Constants.mTransformer.forward(IValue.from(inputTensorTransformer), IValue.from(inputTensorTransformer2));
            if (result.isTuple()) {
                // Get the tuple and extract the tensors
                IValue[] outputs = result.toTuple();
                Tensor prediction_tensor = outputs[0].toTensor();
                Tensor target = outputs[1].toTensor();
                prediction = prediction_tensor.getDataAsLongArray();
                int differenceCount = 0;
                for (int i = 0; i < embeddings.length; i++) {
                    if (embeddings[i] != prediction[i]) {
                        differenceCount++;
                    }
                }
//                    Log.d("tbt", "input: " + Arrays.toString(data));
                Utils.log("before: " + Arrays.toString(embeddings));
                Utils.log("after: " + Arrays.toString(prediction));
                Utils.log("difference after recovery: " + differenceCount);
                inputTensorTransformer = Tensor.fromBlob(prediction, new long[]{1, 64});
                inputTensorTransformer2 = Tensor.fromBlob(prediction, new long[]{1, 64});
            }
        }

        // receiver t5 transformer recover
        final long inferenceTime_transformer_recover = SystemClock.elapsedRealtime() - startTime_transformer_recover;
        Constants.Receiver_Latency_Str = Constants.Receiver_Latency_Str + "receiver transformer recover (ms): " + inferenceTime_transformer_recover + "\n";
        Utils.log("Receiver_Latency_Str: " + Constants.Receiver_Latency_Str);

        // save embedding sequence recovered
        if (Constants.allowLog) {
            FileOperations.writetofile(MainActivity.av, Arrays.toString(prediction),
                    Utils.genName(Constants.SignalType.Rx_Embedding_Recovered, 0) + ".txt");
        }
        return prediction;
    }

    // shared by all protocols
    public static void imageSendPrepare(Bitmap mBitmap, ImageView mImageView, String TaskID) {
        // sender shows the scaled image in left image viewer
        mImageView.post(new Runnable() {
            @Override
            public void run() {
                mImageView.setImageBitmap(mBitmap);
            }
        });

        // save the groundtruth bitmap (scaled from camera image)
        if (Constants.allowLog) {
            FileOperations.saveBitmapToFile(MainActivity.av, mBitmap, Utils.genName(Constants.SignalType.Raw_Input_Bitmap, 0) + ".png");
        }

        Constants.Sender_Latency_Str = ""; // clean up the time
        Constants.Sender_Latency_Str = Constants.Sender_Latency_Str + TaskID + "\n" + Constants.scheme.toString() + "\n"; // TaskID is the time of this sendchirpasync task

        // sender t1 - encode image
        final long startTime = SystemClock.elapsedRealtime();
        // encode
        long[] results = Utils.encode_image(mBitmap);
        // sender t1 - encode image
        final long inferenceTime = SystemClock.elapsedRealtime() - startTime;
        Constants.Sender_Latency_Str = Constants.Sender_Latency_Str + "sender encode image (ms): " + inferenceTime + "\n";
        Utils.log("Sender_Latency_Str: " + Constants.Sender_Latency_Str);

        // save some results, this might introduce extra latency, but here we only use it to test the accuracy
        if (Constants.allowLog) {
            Utils.log("send embedding: " + Arrays.toString(results));

            // save embedding sequence
            FileOperations.writetofile(MainActivity.av, Arrays.toString(results),
                    Utils.genName(Constants.SignalType.Send_Embedding_Sequence, 0) + ".txt");

            // generate VQGAN decoded image ground truth
            Bitmap vqganGtBitmap = Utils.decode_image(results);
            // save VQGAN decoded image ground truth (encoded and decoded via VQGAN)
            FileOperations.saveBitmapToFile(MainActivity.av, vqganGtBitmap, Utils.genName(Constants.SignalType.Sent_Gt_Bitmap, 0) + ".png");
        }
    }

    public static List<Integer> findPeaks(double[] signal) {
        List<Integer> peakIndices = new ArrayList<>();
        for (int i = 1; i < signal.length - 1; i++) {
            if (signal[i] > signal[i - 1] && signal[i] > signal[i + 1]) {
                peakIndices.add(i);
            }
        }
        return peakIndices;
    }

    public static class Peak {
        public final int location;
        public final double width;

        public Peak(int location, double width) {
            this.location = location;
            this.width = width;
        }
    }

    public static List<Peak> findPeaks2(double[] y) {
        List<Peak> peaks = new ArrayList<>();
        DescriptiveStatistics stats = new DescriptiveStatistics();

        for (int i = 1; i < y.length - 1; i++) {
            if (y[i] > y[i - 1] && y[i] > y[i + 1]) {
                // Peak found
                int location = i;
                double height = y[i];

                // Estimate the width of the peak
                int leftBase = findBase(y, i, -1);
                int rightBase = findBase(y, i, 1);
                double width = rightBase - leftBase;

                peaks.add(new Peak(location, width));
                stats.addValue(width);
            }
        }

        return peaks;
    }

    public static double[] linspace2(double start, double end, int num) {
        double[] result = new double[num];
        double step = (end - start) / (num - 1);
        for (int i = 0; i < num; i++) {
            result[i] = start + (i * step);
        }
        return result;
    }

    private static int findBase(double[] y, int peakIndex, int direction) {
        int index = peakIndex;
        while (index >= 0 && index < y.length && y[index] > y[peakIndex] / 2) {
            index += direction;
        }
        return Math.max(0, Math.min(index, y.length - 1));
    }



    public static double[] convertIntToDouble(int[] intArray) {
        double[] doubleArray = new double[intArray.length];
        for (int i = 0; i < intArray.length; i++) {
            doubleArray[i] = (double) intArray[i];
        }
        return doubleArray;
    }

    public static int findFirstValidMaxima(double[] signal) {
        List<Integer> locs = findPeaks(signal);
        if (locs.isEmpty()) {
            return -1;
        }

        double maxPeak = Double.NEGATIVE_INFINITY;
        for (int loc : locs) {
            if (signal[loc] > maxPeak) {
                maxPeak = signal[loc];
            }
        }

        double threshold = Constants.findPeakHeightThreshold * maxPeak;
        int minDistance = Constants.findPeakMinDistanceThreshold;
        List<Integer> filteredLocs = new ArrayList<>();

        for (int loc : locs) {
            if (signal[loc] >= threshold) {
                boolean tooClose = false;
                for (int filteredLoc : filteredLocs) {
                    if (Math.abs(filteredLoc - loc) < minDistance) {
                        tooClose = true;
                        break;
                    }
                }
                if (!tooClose) {
                    filteredLocs.add(loc);
                }
            }
        }
        return filteredLocs.isEmpty() ? -1 : (int)filteredLocs.get(0);
    }



    public static int findFirstValidMaxima2(double[] signal, int lastPeakValue, int th) {
        double[] to_process_signal;
        int to_add = 0;
        if (lastPeakValue == -1) {
            to_process_signal = signal;
        } else {
            to_process_signal = Utils.segment(signal, Math.max(0, lastPeakValue-th), Math.min(signal.length-1, lastPeakValue+th));
            to_add = lastPeakValue-th;
        }

        List<Integer> locs = findPeaks(to_process_signal);

        if (locs.isEmpty()) {
            double[] res = max_idx(to_process_signal);
            int loc = 0;
            loc = (int) (res[0] + to_add);
            return loc;
        } else {
            double maxPeak = Double.NEGATIVE_INFINITY;
            for (int loc : locs) {
                if (to_process_signal[loc] > maxPeak) {
                    maxPeak = to_process_signal[loc];
                }
            }
            double threshold = Constants.findPeakHeightThreshold * maxPeak;
            int minDistance = Constants.findPeakMinDistanceThreshold;
            List<Integer> filteredLocs = new ArrayList<>();
            for (int loc : locs) {
                if (to_process_signal[loc] >= threshold) {
                    boolean tooClose = false;
                    for (int filteredLoc : filteredLocs) {
                        if (Math.abs(filteredLoc - loc) < minDistance) {
                            tooClose = true;
                            break;
                        }
                    }
                    if (!tooClose) {
                        filteredLocs.add(loc);
                    }
                }
            }
            return filteredLocs.isEmpty() ? -1 : (int)filteredLocs.get(0) + to_add;
        }
    }

    public static double[] padSignal(double[] signal) {
        int n = signal.length;
        int nextPowerOf2 = 1;
        while (nextPowerOf2 < n) {
            nextPowerOf2 *= 2;
        }
        double[] paddedSignal = new double[nextPowerOf2];
        System.arraycopy(signal, 0, paddedSignal, 0, n);
        return paddedSignal;
    }

    public static double[] computeHilbertTransform(double[] signal) {
        double[] paddedSignal = padSignal(signal);
        int n = paddedSignal.length;
        FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);

        // Compute FFT of the input signal
        Complex[] fftResult = fft.transform(paddedSignal, TransformType.FORWARD);

        // Create the Hilbert transform multiplier
        Complex[] hilbertMultiplier = new Complex[n];
        hilbertMultiplier[0] = Complex.ZERO;
        for (int i = 1; i < (n + 1) / 2; i++) {
            hilbertMultiplier[i] = Complex.I;
        }
        for (int i = (n + 1) / 2; i < n; i++) {
            hilbertMultiplier[i] = Complex.ZERO;
        }
        if (n % 2 == 0) {
            hilbertMultiplier[n / 2] = Complex.ZERO;
        }

        // Apply the Hilbert transform multiplier
        for (int i = 0; i < n; i++) {
            fftResult[i] = fftResult[i].multiply(hilbertMultiplier[i]);
        }

        // Compute the inverse FFT
        Complex[] ifftResult = fft.transform(fftResult, TransformType.INVERSE);

        // Extract the imaginary part (the Hilbert transform) and remove padding
        double[] hilbertTransform = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            hilbertTransform[i] = ifftResult[i].getImaginary();
        }

        return hilbertTransform;
    }

    public static double[] computeAnalyticSignalMagnitude(double[] signal) {
        // Compute Hilbert Transform
        double[] hilbert = computeHilbertTransform(signal);

        // Compute magnitude of the analytic signal
        double[] magnitude = new double[signal.length];
        for (int i = 0; i < signal.length; i++) {
            magnitude[i] = Math.sqrt(signal[i] * signal[i] + hilbert[i] * hilbert[i]);
        }

        return magnitude;
    }

    public static double[] interp1(double[] x, double[] v, double[] xq) {
        double[] vq = new double[xq.length];

        // Edge cases handling
        for (int i = 0; i < xq.length; i++) {
            if (xq[i] <= x[0]) {
                vq[i] = v[0];
            } else if (xq[i] >= x[x.length - 1]) {
                vq[i] = v[v.length - 1];
            } else {
                // Binary search to find the interval
                int idx = Arrays.binarySearch(x, xq[i]);
                if (idx < 0) {
                    idx = -idx - 2;
                }
                // Linear interpolation formula
                vq[i] = v[idx] + (v[idx + 1] - v[idx]) * (xq[i] - x[idx]) / (x[idx + 1] - x[idx]);
            }
        }

        return vq;
    }


    public static double[] movingMedian(double[] data, int windowSize) {
        Median median = new Median();
        double[] smoothed = new double[data.length];

        for (int i = 0; i < data.length; i++) {
            int start = Math.max(0, i - windowSize / 2);
            int end = Math.min(data.length, i + windowSize / 2 + 1);
            double[] window = Arrays.copyOfRange(data, start, end);
            smoothed[i] = median.evaluate(window);
        }

        return smoothed;
    }

    public static double[] movingMean(double[] data, int windowSize) {
        double[] smoothed = new double[data.length];

        for (int i = 0; i < data.length; i++) {
            int start = Math.max(0, i - windowSize / 2);
            int end = Math.min(data.length, i + windowSize / 2 + 1);
            double sum = 0;
            for (int j = start; j < end; j++) {
                sum += data[j];
            }
            smoothed[i] = sum / (end - start);
        }

        return smoothed;
    }

    public static double[] fillMissing(double[] data) {
        SplineInterpolator interpolator = new SplineInterpolator();
        List<Double> xValues = new ArrayList<>();
        List<Double> yValues = new ArrayList<>();

        for (int i = 0; i < data.length; i++) {
            if (!Double.isNaN(data[i])) {
                xValues.add((double) i);
                yValues.add(data[i]);
            }
        }

        double[] x = xValues.stream().mapToDouble(Double::doubleValue).toArray();
        double[] y = yValues.stream().mapToDouble(Double::doubleValue).toArray();

        PolynomialSplineFunction function = interpolator.interpolate(x, y);

        double[] filled = new double[data.length];
        for (int i = 0; i < data.length; i++) {
            filled[i] = Double.isNaN(data[i]) ? function.value(i) : data[i];
        }

        return filled;
    }

    public static double[] previousInterpolate(double[] originalIndex, double[] dataToSubsample, double[] newIndex) {
        double[] result = new double[newIndex.length];
        int j = 0;

        for (int i = 0; i < newIndex.length; i++) {
            while (j < originalIndex.length && newIndex[i] >= originalIndex[j]) {
                j++;
            }
            if (j > 0) {
                result[i] = dataToSubsample[j - 1];
            } else {
                result[i] = dataToSubsample[0];
            }
        }
        return result;
    }

    public static void datacollection_generate_delaymap() {
        int size = 1 + Constants.datacollection_times * Constants.datacollection_image_count * Constants.all_datacollection_schemes.length;
        int[] delay_map = new int[size];
        int accumulate_delay = 0;
        int i = 0;
        while (i < delay_map.length) {
            if (i == 0) {
                accumulate_delay += Constants.datacollection_init_delay_time;
                delay_map[i] = accumulate_delay;
                i++;
                continue;
            }
            for (int k = 0; k < Constants.datacollection_image_count; k++) {
                for (String scheme : Constants.all_datacollection_schemes) {
                    for (int j = 0; j < Constants.datacollection_times; j++) {
                        if (scheme == "proposed") {
                            accumulate_delay += Constants.datacollection_proposed_time;
                        } else if (scheme == "ofdm_adapt") {
                            accumulate_delay += Constants.datacollection_ofdm_adapt_time;
                        } else if (scheme == "ofdm_wo_adapt") {
                            accumulate_delay += Constants.datacollection_ofdm_wo_adapt_time;
                        } else if (scheme == "css") {
                            accumulate_delay += Constants.datacollection_css_time;
                        }
                        if (j == Constants.datacollection_times-1) {
                            accumulate_delay += Constants.datacollection_mode_switch_time;
                        }
                        delay_map[i] = accumulate_delay;
                        i++;
                        if (i >= delay_map.length) {
                            break;
                        }
                    }
                }
            }
        }
        Constants.datacollection_time_delay_map = delay_map;
    }

    public static void datacollection_generate_time_out_map() {
        int size = 1 + Constants.datacollection_times * Constants.datacollection_image_count * Constants.all_datacollection_schemes.length;
        int[] delay_map = new int[size-1];
        char[] receiver_res = new char[size-1];
        for (int p = 0; p < receiver_res.length; p++) {
            receiver_res[p] = '0';
        }
        Constants.datacollection_receiver_res = receiver_res;
        int accumulate_delay = 0;
        int i = 0;
        while (i < delay_map.length) {
            for (int k = 0; k < Constants.datacollection_image_count; k++) {
                for (String scheme : Constants.all_datacollection_schemes) {
                    for (int j = 0; j < Constants.datacollection_times; j++) {

                        if (i == 0) {
                            accumulate_delay += Constants.datacollection_init_delay_time;
                            if (scheme == "proposed") {
                                accumulate_delay += Constants.datacollection_proposed_time;
                            } else if (scheme == "ofdm_adapt") {
                                accumulate_delay += Constants.datacollection_ofdm_adapt_time;
                            } else if (scheme == "ofdm_wo_adapt") {
                                accumulate_delay += Constants.datacollection_ofdm_wo_adapt_time;
                            } else if (scheme == "css") {
                                accumulate_delay += Constants.datacollection_css_time;
                            }
                            delay_map[i] = accumulate_delay - Constants.receive_time_offset;
                            i++;
                            if (i >= delay_map.length) {
                                break;
                            }
                            continue;
                        }

                        if (scheme == "proposed") {
                            accumulate_delay += Constants.datacollection_proposed_time;
                        } else if (scheme == "ofdm_adapt") {
                            accumulate_delay += Constants.datacollection_ofdm_adapt_time;
                        } else if (scheme == "ofdm_wo_adapt") {
                            accumulate_delay += Constants.datacollection_ofdm_wo_adapt_time;
                        } else if (scheme == "css") {
                            accumulate_delay += Constants.datacollection_css_time;
                        }
                        if (j == 0) {
                            accumulate_delay += Constants.datacollection_mode_switch_time;
                        }

                        delay_map[i] = accumulate_delay - Constants.receive_time_offset;
                        i++;
                        if (i >= delay_map.length) {
                            break;
                        }
                    }
                }
            }
        }

        Constants.datacollection_time_out_map = delay_map;
    }

    public static boolean check_pass_timeout() {
        if (SystemClock.elapsedRealtime() - Constants.datacollection_receive_start_time > Constants.datacollection_time_out_map[Constants.datacollection_current_instance_index]*1000) {
            return true;
        }
        return false;
    }

    public static void update_receiver_res(boolean issuccess) {
        if (issuccess) {
            Constants.datacollection_receiver_res[Constants.datacollection_current_instance_index] = 'Y';
        } else {
            Constants.datacollection_receiver_res[Constants.datacollection_current_instance_index] = 'X';
        }
    }

    public static String get_receiver_res_str() {

        if (Constants.datacollection_receiver_res == null || Constants.datacollection_receiver_res.length == 0) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Constants.datacollection_receiver_res.length; i++) {
            sb.append(Constants.datacollection_receiver_res[i]);
            if (i < Constants.datacollection_receiver_res.length - 1) {
                sb.append('_');
            }
        }

        return sb.toString();
    }

    public static void reset_sensor() {
        Constants.acc = new LinkedList<>();
        Constants.gyro = new LinkedList<>();
        Constants.sensorFlag = true;
    }

    public static void stop_sensor() {
        Constants.sensorFlag = false;
    }




    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public static native String decode(String bits, int poly1, int poly2, int constraint);
    public static native String encode(String bits, int poly1, int poly2, int constraint);
    public static native double[] fftnative_double(double data[], int N);
    public static native double[] fftnative_short(short data[], int N);
    public static native double[][] fftcomplexinoutnative_double(double data[][], int N);
    public static native double[][] fftcomplexoutnative_double(double data[], int N);
    public static native double[][] fftcomplexoutnative_short(short data[], int N);
    public static native double[] ifftnative(double data[][]);
    public static native double[][] ifftnative2(double data[][]);
    public static native void conjnative(double[][] data);
    public static native double[][] timesnative(double[][] data1,double[][] data2);
    public static native double[][] dividenative(double[][] data1,double[][] data2);
    public static native double[] bandpass(double[] data);
    public static native double[] fir(double[] data, double[] h);
}

package sky.toilettes;

import com.pi4j.io.i2c.I2CDevice;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import sky.housecommon.Logger;
import sky.housecommon.PricingPeriod;

public class FourLetterPhatManager extends AbstractI2CManager
{
    private static int[] data=new int[64];
    private static final int ADDRESS=0x70;
    private static final I2CDevice DEVICE;
    public static final int[] SPACE=            new int[]{  0,0,0,0,    0,0,0,0,    0,0,0,0,    0,0,0,0};
    public static final int[] HYPHEN=           new int[]{  0,0,0,0,    0,0,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] DEGREE=           new int[]{  1,1,0,0,    0,1,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] RIGHT_ARROW=      new int[]{  0,0,0,0,    1,1,0,1,    1,0,0,1,    0,0,0,0};
    public static final int[] A=                new int[]{  1,1,1,0,    1,1,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] B=                new int[]{  1,1,1,1,    0,0,0,1,    0,1,0,0,    1,0,0,0};
    public static final int[] C=                new int[]{  1,0,0,1,    1,1,0,0,    0,0,0,0,    0,0,0,0};
    public static final int[] D=                new int[]{  1,1,1,1,    0,0,0,0,    0,1,0,0,    1,0,0,0};
    public static final int[] H=                new int[]{  0,1,1,0,    1,1,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] P=                new int[]{  1,1,0,0,    1,1,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] R=                new int[]{  1,1,0,0,    1,1,1,1,    0,0,0,0,    0,1,0,0};
    public static final int[] W=                new int[]{  0,1,1,0,    1,1,0,0,    0,0,0,1,    0,1,0,0};
    public static final int[] ZERO=             new int[]{  1,1,1,1,    1,1,0,0,    0,0,0,0,    0,0,0,0};
    public static final int[] ONE=              new int[]{  0,1,1,0,    0,0,0,0,    0,0,1,0,    0,0,0,0};
    public static final int[] TWO=              new int[]{  1,1,0,1,    1,0,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] THREE=            new int[]{  1,1,1,1,    0,0,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] FOUR=             new int[]{  0,1,1,0,    0,1,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] FIVE=             new int[]{  1,0,1,1,    0,1,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] SIX=              new int[]{  1,0,1,1,    1,1,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] SEVEN=            new int[]{  1,1,1,0,    0,1,0,0,    0,0,0,0,    0,0,0,0};
    public static final int[] EIGHT=            new int[]{  1,1,1,1,    1,1,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] NINE=             new int[]{  1,1,1,1,    0,1,1,1,    0,0,0,0,    0,0,0,0};
    public static final int[] DECIMAL_POINT=    new int[]{  0,0,0,0,    0,0,0,0,    0,0,0,0,    0,0,1,0};
    private static final NumberFormat NUMBER_FORMAT=new DecimalFormat("#0.0");

    static
    {
        I2CDevice device=null;
        try
        {
            device=BUS.getDevice(ADDRESS);
        }
        catch(IOException e)
        {
            Logger.LOGGER.error("Unable to get device for FourLetterPhat ("+e.toString()+")");
            System.exit(1);
        }
        DEVICE=device;
        try
        {
            byte[] array=new byte[0];
            DEVICE.write(0x21,array);//Turn on the oscillator
            DEVICE.write(0x81,array);//Turn display on with no blinking
            DEVICE.write(0xEF,array);//Set display to full brightness
        }
        catch(IOException e)
        {
            Logger.LOGGER.error("Unable to configure device for FourLetterPhat ("+e.toString()+")");
            System.exit(1);
        }
    }

    private FourLetterPhatManager()
    {
    }

    public static void print(int rank,int[] segments)
    {
        System.arraycopy(segments,0,data,16*rank,16);
        display();
    }

    public static void print(int[] segments1,int[] segments2,int[] segments3,int[] segments4)
    {
        System.arraycopy(segments1,0,data,0,16);
        System.arraycopy(segments2,0,data,16,16);
        System.arraycopy(segments3,0,data,32,16);
        System.arraycopy(segments4,0,data,48,16);
        display();
    }

    public static void printTemperature(double temperature)
    {
        try
        {
            if(temperature>=10d)
            {
                String s=String.valueOf(temperature+1e-3d);
                int digit1=Integer.parseInt(s.substring(0,1));
                int digit2=Integer.parseInt(s.substring(1,2));
                int digit3=Integer.parseInt(s.substring(3,4));
                print(getData(digit1),getDataDP(digit2),getData(digit3),Toilettes.HEATER_THREAD.isHeaterOn()?appendDigit(DEGREE,DECIMAL_POINT):DEGREE);
            }
            else
            {
                String s=String.valueOf(temperature+1e-3d);
                int digit1=Integer.parseInt(s.substring(0,1));
                int digit2=Integer.parseInt(s.substring(2,3));
                print(SPACE,getDataDP(digit1),getData(digit2),DEGREE);
            }
        }
        catch(NumberFormatException e)
        {
            Logger.LOGGER.error("Unable to format the temperature "+temperature+" ("+e.toString()+")");
        }
    }

    public static void printCurrentTime()
    {
        GregorianCalendar calendar=new GregorianCalendar();
        int hour=calendar.get(Calendar.HOUR_OF_DAY);
        int minute=calendar.get(Calendar.MINUTE);
        if(hour<10)
            print(SPACE,getDataDP(hour),getData(minute/10),getData(minute%10));
        else
            print(getData(hour/10),getDataDP(hour%10),getData(minute/10),getData(minute%10));
    }

    public static void printSetPoint(double setPoint)
    {
        try
        {
            if(setPoint>=10d)
            {
                String s=String.valueOf(setPoint+1e-3d);
                int digit1=Integer.parseInt(s.substring(0,1));
                int digit2=Integer.parseInt(s.substring(1,2));
                int digit3=Integer.parseInt(s.substring(3,4));
                print(RIGHT_ARROW,getData(digit1),getDataDP(digit2),getData(digit3));
            }
            else
            {
                String s=String.valueOf(setPoint+1e-3d);
                int digit1=Integer.parseInt(s.substring(0,1));
                int digit2=Integer.parseInt(s.substring(2,3));
                print(RIGHT_ARROW,SPACE,getDataDP(digit1),getData(digit2));
            }
        }
        catch(NumberFormatException e)
        {
            Logger.LOGGER.error("Unable to format the set point "+setPoint+" ("+e.toString()+")");
        }
    }

    public static void printPricingPeriod(PricingPeriod pricingPeriod)
    {
        print(pricingPeriod.isBlueDay()?B:pricingPeriod.isWhiteDay()?W:R,HYPHEN,H,pricingPeriod.isOffPeakHourPeriod()?C:P);
    }

    private static int[] getData(int digit)
    {
        if(digit==0)
            return ZERO;
        else
            if(digit==1)
                return ONE;
            else
                if(digit==2)
                    return TWO;
                else
                    if(digit==3)
                        return THREE;
                    else
                        if(digit==4)
                            return FOUR;
                        else
                            if(digit==5)
                                return FIVE;
                            else
                                if(digit==6)
                                    return SIX;
                                else
                                    if(digit==7)
                                        return SEVEN;
                                    else
                                        if(digit==8)
                                            return EIGHT;
                                        else
                                            if(digit==9)
                                                return NINE;
        return SPACE;
    }

    private static int[] getDataDP(int digit)
    {
        return appendDigit(getData(digit),DECIMAL_POINT);
    }

    private static int[] appendDigit(int[] digit1,int[] digit2)
    {
        int[] digit=new int[digit1.length];
        for(int i=0;i<digit.length;i++)
            digit[i]=digit1[i]==1||digit2[i]==1?1:0;
        return digit;
    }

    private static void display()
    {
        try
        {
            byte[] bytes=new byte[8];
            for(int digit=0;digit<4;digit++)
                for(int bit=0;bit<8;bit++)
                {
                    bytes[2*digit]|=(data[16*digit+bit]==1?1:0)<<bit;
                    bytes[2*digit+1]|=(data[16*digit+bit+8]==1?1:0)<<bit;
                }
//            Logger.LOGGER.debug("Display content is "+Arrays.toString(bytes));
            DEVICE.write(0x00,bytes);
        }
        catch(IOException e)
        {
            Logger.LOGGER.error("Unable to write to the device for FourLetterPhat ("+e.toString()+")");
        }
    }
}

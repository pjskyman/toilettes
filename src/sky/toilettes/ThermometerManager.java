package sky.toilettes;

import com.pi4j.io.i2c.I2CDevice;
import java.io.IOException;
import sky.housecommon.Logger;

public class ThermometerManager extends AbstractI2CManager
{
    private static final int ADDRESS=0x18;
    private static final I2CDevice DEVICE;

    static
    {
        I2CDevice device=null;
        try
        {
            device=BUS.getDevice(ADDRESS);
        }
        catch(IOException e)
        {
            Logger.LOGGER.error("Unable to get device for thermometer ("+e.toString()+")");
            System.exit(1);
        }
        DEVICE=device;
        try
        {
            DEVICE.write(0x01,new byte[2]);//Continuous conversion mode, Power-up default
            DEVICE.write(0x08,(byte)0x03);//résolution maximale, c'est-à-dire 0.0625°C par incrément
        }
        catch(IOException e)
        {
            Logger.LOGGER.error("Unable to configure device for thermometer ("+e.toString()+")");
            System.exit(1);
        }
    }

    private ThermometerManager()
    {
    }

    public static double getTemperature()
    {
        try
        {
            byte[] data=new byte[2];
            DEVICE.read(0x05,data,0,data.length);
            int temperature=((data[0]&0x1F)*256+(data[1]&0xFF));
            if(temperature>4095)
                temperature-=8192;
//            Logger.LOGGER.info("Temperature is "+temperature*.0625d+"°C");
            return temperature*.0625d;
        }
        catch(IOException e)
        {
            Logger.LOGGER.error("Unable to get temperature ("+e.toString()+")");
            return Double.NaN;
        }
    }
}

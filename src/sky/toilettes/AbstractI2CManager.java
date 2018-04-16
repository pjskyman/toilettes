package sky.toilettes;

import com.pi4j.io.i2c.I2CBus;
import com.pi4j.io.i2c.I2CFactory;
import com.pi4j.io.i2c.I2CFactory.UnsupportedBusNumberException;
import java.io.IOException;

public abstract class AbstractI2CManager
{
    protected static final I2CBus BUS;

    static
    {
        I2CBus bus=null;
        try
        {
            bus=I2CFactory.getInstance(I2CBus.BUS_1);
        }
        catch(IOException|UnsupportedBusNumberException e)
        {
            Logger.LOGGER.error("Unable to get I2C bus ("+e.toString()+")");
            System.exit(1);
        }
        BUS=bus;
    }
}

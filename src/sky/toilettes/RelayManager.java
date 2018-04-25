package sky.toilettes;

import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.PinState;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.impl.GpioControllerImpl;
import sky.program.Duration;

public class RelayManager
{
    private static GpioController gpio=null;
    private static final String RELAY="relay";

    private RelayManager()
    {
    }

    public static void on()
    {
        synchronized(RELAY)
        {
            if(gpio!=null)
                return;
            gpio=new GpioControllerImpl();
            GpioPinDigitalOutput pin=(GpioPinDigitalOutput)gpio.getProvisionedPin(RELAY);
            if(pin!=null)
                pin.high();
            else
            {
                pin=onImpl();
                if(pin==null)
                    return;
            }
            pin.setPullResistance(PinPullResistance.PULL_UP);
            pin.setShutdownOptions(true,PinState.LOW);
        }
    }

    private static GpioPinDigitalOutput onImpl()
    {
        for(int i=0;i<100;i++)
            try
            {
                return gpio.provisionDigitalOutputPin(RaspiPin.GPIO_02,RELAY,PinState.HIGH);
            }
            catch(RuntimeException e)
            {
                Logger.LOGGER.error("Error when opening the pin 2 ("+e.toString()+")");
                try
                {
                    Thread.sleep(Duration.of(500).millisecond());
                    gpio.shutdown();
                    gpio=null;
                    Runtime.getRuntime().gc();
                    gpio=new GpioControllerImpl();
                    Thread.sleep(Duration.of(500).millisecond());
                }
                catch(InterruptedException ex)
                {
                    return null;
                }
                catch(Exception ex)
                {
                    Logger.LOGGER.error("Error when renewing GpioController ("+ex.toString()+")");
                    return null;
                }
                if(i<99)
                    Logger.LOGGER.info("Attempting one more time to open the pin 2");
            }
        return null;
    }

    public static void off()
    {
        synchronized(RELAY)
        {
            if(gpio==null)
                return;
            gpio.shutdown();
            gpio=null;
        }
    }
}

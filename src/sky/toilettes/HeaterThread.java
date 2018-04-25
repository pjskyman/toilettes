package sky.toilettes;

import java.util.Queue;
import sky.program.Duration;

public class HeaterThread extends Thread
{
    private double nextRatio;
    private double currentRatio;
    private boolean currentStatus;
    private boolean cycleInterrupted;
    private static long lastMeasureTime=0L;
    private static final long CYCLE_TIME=Duration.of(10).minute();
    private static final long MINIMAL_STATE_TIME=Duration.of(10).second();
    private static final Object LOCK_OBJECT=new Object();
    private static final double FULL_RATIO_TEMPERATURE_OFFSET=-.5d;

    public HeaterThread()
    {
        nextRatio=currentRatio=0d;
        currentStatus=cycleInterrupted=false;
    }

    public void updateEnvironment(double temperature,double setPoint,boolean heaterOn,Queue<Temperature> temperatureQueue)
    {
        double ratio=(setPoint-temperature)/(-FULL_RATIO_TEMPERATURE_OFFSET);
        if(ratio>1d)
            ratio=1d;
        if(ratio>0d)
            ratio=Math.sqrt(ratio);
        Logger.LOGGER.info("Temperature="+temperature+"°C, set point="+setPoint+"°C, ratio="+ratio);
        long now=System.currentTimeMillis();
        if(now-lastMeasureTime>Duration.of(5).second())
        {
            temperatureQueue.offer(new Temperature(now,temperature,setPoint,Math.max(0d,ratio),heaterOn));
//            Logger.LOGGER.info("Mise en file d'une nouvelle mesure (taille="+temperatureQueue.size()+")");
            lastMeasureTime=now;
        }
        nextRatio=Math.max(0d,ratio);
        synchronized(LOCK_OBJECT)
        {
            if(cycleInterrupted)
                return;
            if(ratio<=currentRatio-.5d&&currentStatus)
                cycleInterrupted=true;
            else
                if(nextRatio>=currentRatio+.5d&&!currentStatus)
                    cycleInterrupted=true;
        }
    }

    public boolean isHeaterOn()
    {
        return currentStatus;
    }

    @Override
    public void run()
    {
        Logger.LOGGER.info("Heater is currently off");
        try
        {
            while(true)
            {
                currentRatio=nextRatio;
                long timeToHeat=(long)((double)CYCLE_TIME*currentRatio);
                timeToHeat=timeToHeat<=0L?0L:Math.max(timeToHeat,MINIMAL_STATE_TIME);
                timeToHeat=timeToHeat>=CYCLE_TIME?CYCLE_TIME:Math.min(timeToHeat,CYCLE_TIME-MINIMAL_STATE_TIME);
                Logger.LOGGER.info("Beginning a new cycle with "+timeToHeat/1000L+" s of heating");
                if(timeToHeat==0L)
                {
                    if(currentStatus)
                    {
                        currentStatus=false;
                        RelayManager.off();
                        Logger.LOGGER.info("Heater is currently off");
                    }
                }
                else
                {
                    if(!currentStatus)
                    {
                        currentStatus=true;
                        RelayManager.on();
                        Logger.LOGGER.info("Heater is currently on");
                    }
                    if(smartSleep(timeToHeat))
                    {
                        Logger.LOGGER.info("Prematurely aborting the current cycle");
                        continue;
                    }
                    if(timeToHeat<CYCLE_TIME)
                    {
                        currentStatus=false;
                        RelayManager.off();
                        Logger.LOGGER.info("Heater is currently off");
                    }
                }
                if(smartSleep(CYCLE_TIME-timeToHeat))
                    Logger.LOGGER.info("Prematurely aborting the current cycle");
            }
        }
        catch(InterruptedException e)
        {
        }
    }

    private boolean smartSleep(long time) throws InterruptedException
    {
//        Thread.sleep(time);
//        if(true)
//            return false;
        long startTime=System.currentTimeMillis();
        while(System.currentTimeMillis()-startTime<time)
        {
            synchronized(LOCK_OBJECT)
            {
                if(cycleInterrupted)
                    break;
            }
            Thread.sleep(Duration.of(100).millisecond());
        }
        synchronized(LOCK_OBJECT)
        {
            if(cycleInterrupted)
            {
                cycleInterrupted=false;
                return true;
            }
            else
                return false;
        }
    }
}

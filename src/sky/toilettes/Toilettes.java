package sky.toilettes;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.ToDoubleFunction;
import sky.housecommon.Database;
import sky.housecommon.InstantaneousConsumption;
import sky.housecommon.Logger;
import sky.housecommon.NotAvailableDatabaseException;
import sky.housecommon.PricingPeriod;
import sky.housecommon.Temperature;
import sky.program.Duration;

public class Toilettes
{
    private static long lastPricingPeriodVerificationTime=0L;
    private static PricingPeriod currentPricingPeriod=PricingPeriod.UNKNOWN;
    private static final long PRICING_PERIOD_VERIFICATION_DELAY=Duration.of(5).second();
    protected static final HeaterThread HEATER_THREAD=new HeaterThread();//protected pour accès dans FourLetterPhatManager
    private static final ToDoubleFunction<PricingPeriod> SETPOINT_OFFSET_PROVIDER=pricingPeriod->
    {
        if(pricingPeriod==PricingPeriod.UNKNOWN)
            return -10d;
        if(pricingPeriod==PricingPeriod.RED_DAY_PEAK_HOUR)
            return -5d;
        return -(pricingPeriod.getPrice()-PricingPeriod.BLUE_DAY_OFF_PEAK_HOUR.getPrice())*35.25d;
    };
    private static final Queue<Temperature> TEMPERATURES;
    private static final DatabasePopulator DATABASE_POPULATOR;

    static
    {
        TEMPERATURES=new ConcurrentLinkedQueue<>();
        DATABASE_POPULATOR=new DatabasePopulator();
        DATABASE_POPULATOR.start();
    }

    public static void main(String[] args)
    {
        new ExternalController(new ExternallyControlled()
        {
            public void on()
            {
                Logger.LOGGER.info("External controller said on");
                HEATER_THREAD.setOverridingOff(false);
            }

            public void off()
            {
                Logger.LOGGER.info("External controller said off");
                HEATER_THREAD.setOverridingOff(true);
            }

            public void set(double setPoint,double hours)
            {
                Logger.LOGGER.info("External controller said set "+setPoint+" "+hours);
                SetPointManager.setOverridingSetPoint(setPoint,hours);
            }

            public void reset()
            {
                Logger.LOGGER.info("External controller said reset");
                SetPointManager.resetOverridingSetPoint();
            }
        });
        boolean started=false;
        try
        {
            int count=0;
            while(true)
                try
                {
                    long now=System.currentTimeMillis();
                    if(now-lastPricingPeriodVerificationTime>PRICING_PERIOD_VERIFICATION_DELAY)
                    {
                        refreshPricingPeriod();
                        lastPricingPeriodVerificationTime=now;
                    }
                    double temperature=ThermometerManager.getTemperature();
                    double correctedSetPoint=Math.max(0d,SetPointManager.getCurrentSetPoint()+SETPOINT_OFFSET_PROVIDER.applyAsDouble(currentPricingPeriod));
                    if(++count%6==3)
                        FourLetterPhatManager.printSetPoint(correctedSetPoint);
                    else
                        if(count%6==4)
                            FourLetterPhatManager.printCurrentTime();
                        else
                            if(count%6==5)
                                FourLetterPhatManager.printPricingPeriod(currentPricingPeriod);
                            else
                                FourLetterPhatManager.printTemperature(temperature);
                    HEATER_THREAD.updateEnvironment(temperature,correctedSetPoint,HEATER_THREAD.isHeaterOn(),TEMPERATURES);
                    if(!started)
                    {
                        HEATER_THREAD.start();
                        started=true;
                    }
                    Thread.sleep(Duration.of(1).second());
                }
                catch(InterruptedException e)
                {
                    throw e;
                }
                catch(Exception e)
                {
                    Logger.LOGGER.error("Error in main loop ("+e.toString()+")");
                    Thread.sleep(Duration.of(1).second());
                }
        }
        catch(InterruptedException e)
        {
        }
    }

    private static void refreshPricingPeriod()
    {
        InstantaneousConsumption instantaneousConsumption=Database.getLastInstantaneousConsumption();
        if(instantaneousConsumption==null)
        {
            try
            {
                Thread.sleep(Duration.of(100).millisecond());
            }
            catch(InterruptedException e)
            {
            }
            instantaneousConsumption=Database.getLastInstantaneousConsumption();
            if(instantaneousConsumption==null)
            {
                Logger.LOGGER.error("Unable to get the last measure");
            }
        }
        PricingPeriod oldPricingPeriod=currentPricingPeriod;
        currentPricingPeriod=instantaneousConsumption!=null?instantaneousConsumption.getPricingPeriod():PricingPeriod.UNKNOWN;
        if(currentPricingPeriod!=oldPricingPeriod)
            Logger.LOGGER.info("Now the pricing period is "+currentPricingPeriod.name()+" instead of "+oldPricingPeriod.name());
    }

    private static class DatabasePopulator extends Thread
    {
        private DatabasePopulator()
        {
            super(DatabasePopulator.class.getSimpleName());
        }

        @Override
        public void run()
        {
            while(true)
                try
                {
                    Thread.sleep(Duration.of(100).millisecond());
                    while(!TEMPERATURES.isEmpty())
                    {
                        Temperature temperature=TEMPERATURES.peek();
                        long startTime=System.currentTimeMillis();
                        boolean success=insertTemperatureIntoDatabase(temperature);
                        long totalTime=System.currentTimeMillis()-startTime;
                        if(success)
                        {
                            TEMPERATURES.poll();
//                            Logger.LOGGER.info("Ajout en base de données d'une mesure (taille="+TEMPERATURES.size()+") ["+totalTime+" ms]");
                        }
                        else
                        {
                            Logger.LOGGER.info("\tProblème à l'ajout en base de données d'une mesure (taille="+TEMPERATURES.size()+") ["+totalTime+" ms]");
                            Thread.sleep(Duration.of(100).millisecond());
                        }
                    }
                }
                catch(InterruptedException e)
                {
                }
                catch(Throwable t)
                {
                    t.printStackTrace();
                }
        }

        private boolean insertTemperatureIntoDatabase(Temperature temperature)
        {
            try
            {
                return Database.insertTemperature(temperature);
            }
            catch(NotAvailableDatabaseException e)
            {
                return false;
            }
        }
    }
}

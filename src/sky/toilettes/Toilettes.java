package sky.toilettes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import sky.program.Duration;

public class Toilettes
{
    private static long lastPricingPeriodVerificationTime=0L;
    private static PricingPeriod currentPricingPeriod=PricingPeriod.UNKNOWN;
    private static final long PRICING_PERIOD_VERIFICATION_DELAY=Duration.of(5).second();
    protected static final HeaterThread HEATER_THREAD=new HeaterThread();//protected pour accès dans FourLetterPhatManager
    private static final Map<PricingPeriod,Double> SETPOINT_OFFSETS;
    private static final Queue<Temperature> TEMPERATURES;
    private static final DatabasePopulator DATABASE_POPULATOR;

    static
    {
        TEMPERATURES=new ConcurrentLinkedQueue<>();
        DATABASE_POPULATOR=new DatabasePopulator();
        DATABASE_POPULATOR.start();
        SETPOINT_OFFSETS=new HashMap<>();
        SETPOINT_OFFSETS.put(PricingPeriod.BLUE_DAY_OFF_PEAK_HOUR,Double.valueOf(0d));
        SETPOINT_OFFSETS.put(PricingPeriod.BLUE_DAY_PEAK_HOUR,Double.valueOf(-.6d));
        SETPOINT_OFFSETS.put(PricingPeriod.WHITE_DAY_OFF_PEAK_HOUR,Double.valueOf(-.7d));
        SETPOINT_OFFSETS.put(PricingPeriod.WHITE_DAY_PEAK_HOUR,Double.valueOf(-1.6d));
        SETPOINT_OFFSETS.put(PricingPeriod.RED_DAY_OFF_PEAK_HOUR,Double.valueOf(-2d));
        SETPOINT_OFFSETS.put(PricingPeriod.RED_DAY_PEAK_HOUR,Double.valueOf(-5d));
        SETPOINT_OFFSETS.put(PricingPeriod.UNKNOWN,Double.valueOf(-10d));
    }

    public static void main(String[] args)
    {
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
                    double correctedSetPoint=Math.max(0d,SetPointManager.getCurrentSetPoint()+SETPOINT_OFFSETS.get(currentPricingPeriod).doubleValue());
                    if(++count%5==3)
                        FourLetterPhatManager.printSetPoint(correctedSetPoint);
                    else
                        if(count%5==4)
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
        List<InstantaneousConsumption> instantaneousConsumptions=loadLastInstantaneousConsumption();
        if(instantaneousConsumptions.isEmpty())
        {
            try
            {
                Thread.sleep(Duration.of(100).millisecond());
            }
            catch(InterruptedException e)
            {
            }
            instantaneousConsumptions=loadLastInstantaneousConsumption();
            if(instantaneousConsumptions.isEmpty())
            {
                Logger.LOGGER.error("Unable to get the last measure");
            }
        }
        PricingPeriod oldPricingPeriod=currentPricingPeriod;
        currentPricingPeriod=instantaneousConsumptions.get(instantaneousConsumptions.size()-1).getPricingPeriod();
        if(currentPricingPeriod==oldPricingPeriod)
        {
//            Logger.LOGGER.info("The pricing period is always "+currentPricingPeriod.name());
        }
        else
            Logger.LOGGER.info("Now the pricing period is "+currentPricingPeriod.name()+" instead of "+oldPricingPeriod.name());
    }

    private static List<InstantaneousConsumption> loadLastInstantaneousConsumption()
    {
        try
        {
            try(Connection connection=getConnection1())
            {
                long startTime=System.currentTimeMillis();
                List<InstantaneousConsumption> instantaneousConsumptions=new ArrayList<>();
                try(Statement statement=connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY))
                {
                    try(ResultSet resultSet=statement.executeQuery("SELECT * FROM instantaneous_consumption ORDER BY time DESC LIMIT 1;"))
                    {
                        while(resultSet.next())
                        {
                            long time=resultSet.getLong("time");
                            PricingPeriod pricingPeriod=PricingPeriod.getPricingPeriodForCode(resultSet.getInt("pricingPeriod"));
                            double blueDayOffPeakHourTotal=resultSet.getDouble("blueDayOffPeakHourTotal");
                            double blueDayPeakHourTotal=resultSet.getDouble("blueDayPeakHourTotal");
                            double whiteDayOffPeakHourTotal=resultSet.getDouble("whiteDayOffPeakHourTotal");
                            double whiteDayPeakHourTotal=resultSet.getDouble("whiteDayPeakHourTotal");
                            double redDayOffPeakHourTotal=resultSet.getDouble("redDayOffPeakHourTotal");
                            double redDayPeakHourTotal=resultSet.getDouble("redDayPeakHourTotal");
                            String consumer1Name=resultSet.getString("consumer1Name");
                            int consumer1Consumption=resultSet.getInt("consumer1Consumption");
                            String consumer2Name=resultSet.getString("consumer2Name");
                            int consumer2Consumption=resultSet.getInt("consumer2Consumption");
                            String consumer3Name=resultSet.getString("consumer3Name");
                            int consumer3Consumption=resultSet.getInt("consumer3Consumption");
                            String consumer4Name=resultSet.getString("consumer4Name");
                            int consumer4Consumption=resultSet.getInt("consumer4Consumption");
                            String consumer5Name=resultSet.getString("consumer5Name");
                            int consumer5Consumption=resultSet.getInt("consumer5Consumption");
                            String consumer6Name=resultSet.getString("consumer6Name");
                            int consumer6Consumption=resultSet.getInt("consumer6Consumption");
                            String consumer7Name=resultSet.getString("consumer7Name");
                            int consumer7Consumption=resultSet.getInt("consumer7Consumption");
                            String consumer8Name=resultSet.getString("consumer8Name");
                            int consumer8Consumption=resultSet.getInt("consumer8Consumption");
                            String consumer9Name=resultSet.getString("consumer9Name");
                            int consumer9Consumption=resultSet.getInt("consumer9Consumption");
                            String consumer10Name=resultSet.getString("consumer10Name");
                            int consumer10Consumption=resultSet.getInt("consumer10Consumption");
                            instantaneousConsumptions.add(new InstantaneousConsumption(time,
                                    pricingPeriod,
                                    blueDayOffPeakHourTotal,
                                    blueDayPeakHourTotal,
                                    whiteDayOffPeakHourTotal,
                                    whiteDayPeakHourTotal,
                                    redDayOffPeakHourTotal,
                                    redDayPeakHourTotal,
                                    consumer1Name,
                                    consumer1Consumption,
                                    consumer2Name,
                                    consumer2Consumption,
                                    consumer3Name,
                                    consumer3Consumption,
                                    consumer4Name,
                                    consumer4Consumption,
                                    consumer5Name,
                                    consumer5Consumption,
                                    consumer6Name,
                                    consumer6Consumption,
                                    consumer7Name,
                                    consumer7Consumption,
                                    consumer8Name,
                                    consumer8Consumption,
                                    consumer9Name,
                                    consumer9Consumption,
                                    consumer10Name,
                                    consumer10Consumption));
                        }
                    }
                }
                Collections.reverse(instantaneousConsumptions);
//                Logger.LOGGER.info(instantaneousConsumptions.size()+" rows fetched in "+(System.currentTimeMillis()-startTime)+" ms");
                return instantaneousConsumptions;
            }
        }
        catch(NotAvailableDatabaseException|SQLException e)
        {
            Logger.LOGGER.error("Unable to parse the request response ("+e.toString()+")");
            return new ArrayList<>(0);
        }
    }

    private static Connection getConnection1() throws NotAvailableDatabaseException
    {
        String serverAddress="";
        String serverPort="";
        String databaseName="";
        String user="";
        String password="";
        try(BufferedReader reader=new BufferedReader(new FileReader(new File("database1.ini"))))
        {
            serverAddress=reader.readLine();
            serverPort=reader.readLine();
            databaseName=reader.readLine();
            user=reader.readLine();
            password=reader.readLine();
        }
        catch(IOException e)
        {
            Logger.LOGGER.error("Unable to read database connection infos 1 from the config file ("+e.toString()+")");
        }
        Connection connection=null;
        try
        {
            connection=DriverManager.getConnection("jdbc:mariadb://"+serverAddress+":"+serverPort+"/"+databaseName+"?user="+user+"&password="+password);
//            Logger.LOGGER.info("Connection to SQLite has been established.");
        }
        catch(SQLException e)
        {
            try
            {
                if(connection!=null)
                    connection.close();
            }
            catch(SQLException ex)
            {
            }
            Logger.LOGGER.error(e.toString());
        }
        return connection;
    }

    private static boolean insertIntoDatabase(Temperature temperature) throws NotAvailableDatabaseException
    {
        boolean success=false;
        try(Connection connection=getConnection2())
        {
            try(Statement statement=connection.createStatement(ResultSet.TYPE_FORWARD_ONLY,ResultSet.CONCUR_READ_ONLY))
            {
                int result=statement.executeUpdate("INSERT INTO temperature VALUES ("
                        + temperature.getTime()+","
                        + temperature.getTemperature()+","
                        + temperature.getSetPoint()+","
                        + temperature.getRatio()+","
                        + (temperature.isHeaterOn()?1:0)+");");
                if(result==1)
                    success=true;
            }
        }
        catch(SQLException ex)
        {
            Logger.LOGGER.error(ex.toString());
            throw new NotAvailableDatabaseException(ex);
        }
        return success;
    }

    private static Connection getConnection2() throws NotAvailableDatabaseException
    {
        String serverAddress="";
        String serverPort="";
        String databaseName="";
        String user="";
        String password="";
        try(BufferedReader reader=new BufferedReader(new FileReader(new File("database2.ini"))))
        {
            serverAddress=reader.readLine();
            serverPort=reader.readLine();
            databaseName=reader.readLine();
            user=reader.readLine();
            password=reader.readLine();
        }
        catch(IOException e)
        {
            Logger.LOGGER.error("Unable to read database connection infos 2 from the config file ("+e.toString()+")");
        }
        Connection connection=null;
        try
        {
            connection=DriverManager.getConnection("jdbc:mariadb://"+serverAddress+":"+serverPort+"/"+databaseName+"?user="+user+"&password="+password);
//            Logger.LOGGER.info("Connection to SQLite has been established.");
        }
        catch(SQLException e)
        {
            try
            {
                if(connection!=null)
                    connection.close();
            }
            catch(SQLException ex)
            {
            }
            Logger.LOGGER.error(e.toString());
        }
        return connection;
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
                return insertIntoDatabase(temperature);
            }
            catch(NotAvailableDatabaseException e)
            {
                return false;
            }
        }
    }
}

package sky.toilettes;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import sky.housecommon.Logger;
import sky.netatmo.NetatmoException;
import sky.netatmo.Token;
import sky.netatmo.TokenExpiredException;
import sky.program.Duration;

public class SetPointManager
{
    private static long lastNetatmoVerificationTime=0L;
    private static Token token=null;
    private static String statusMode="N/A";
    private static String dataMode="N/A";
    private static double manualSetPoint=0d;
    private static boolean overridingSetPointEnabled=false;
    private static double overridingSetPoint=0d;
    private static long overridingSetPointStartTime=0L;
    private static double overridingSetPointHours=0d;
    private static final List<Schedule> SCHEDULES=new ArrayList<>(2);
    private static final long NETATMO_VERIFICATION_DELAY=Duration.of(10).minute();

    public static synchronized double getCurrentSetPoint()
    {
        long now=System.currentTimeMillis();
        if(now-lastNetatmoVerificationTime>NETATMO_VERIFICATION_DELAY)
        {
            refreshSchedules();
            lastNetatmoVerificationTime=now;
        }
        try
        {
            if(overridingSetPointEnabled)
            {
                if((double)(now-overridingSetPointStartTime)/3_600_000d>overridingSetPointHours)
                    resetOverridingSetPoint();
                else
                    return overridingSetPoint;
            }
            if(statusMode.equals("manual"))
                return manualSetPoint;
            if(statusMode.equals("off"))
                return 0d;
            if(statusMode.equals("max"))
                return 30d;
            Schedule activeSchedule=SCHEDULES.stream().filter(Schedule::isSelected).findFirst().orElse(SCHEDULES.get(0));
            if(dataMode.equals("away"))
                return activeSchedule.getAway_temp();
            if(dataMode.equals("frost_guard"))
                return activeSchedule.getHg_temp();
            //nous allons maintenant nous baser sur le planning courant pour calculer une consigne aux petits oignons
            SchedulePeriod[] periods=new SchedulePeriod[activeSchedule.getTimes().size()];
            for(int i=0;i<periods.length;i++)
            {
                periods[i]=new SchedulePeriod();
                periods[i].setStartOffset(activeSchedule.getTimes().get(i).getM_offset());
                periods[i].setEndOffset(i<periods.length-1?activeSchedule.getTimes().get(i+1).getM_offset():60*24*7);
                periods[i].setTemperature(getZoneTemperature(activeSchedule,activeSchedule.getTimes().get(i).getZone_id()));
            }
            GregorianCalendar calendar=new GregorianCalendar();
            int biasedDayOfWeek=(calendar.get(Calendar.DAY_OF_WEEK)-2+7)%7;
            int hour=calendar.get(Calendar.HOUR_OF_DAY);
            int minute=calendar.get(Calendar.MINUTE);
            int nowInMinutes=minute+60*hour+24*60*biasedDayOfWeek;
            int second=calendar.get(Calendar.SECOND);
            int currentPeriodIndex;
            for(currentPeriodIndex=periods.length-1;currentPeriodIndex>=0;currentPeriodIndex--)
                if(periods[currentPeriodIndex].getStartOffset()<=nowInMinutes)
                    break;
            SchedulePeriod currentPeriod=periods[currentPeriodIndex];
            double currentPeriodTemperature=currentPeriod.getTemperature();
            //recherche de prochaines montées de température à anticiper
            double adjustedTemperature=currentPeriodTemperature;
            for(int i=currentPeriodIndex+1;i<2*periods.length;i++)
            {
                boolean nextWeek=i>=periods.length;
                SchedulePeriod studiedPeriod=periods[i%periods.length];
                int studiedPeriodOffset=studiedPeriod.getStartOffset()+(nextWeek?60*24*7:0);
                if(studiedPeriodOffset>nowInMinutes+7*60)//trop loin de 7h, on abandonne
                    break;
                double studiedPeriodTemperature=studiedPeriod.getTemperature();
                if(studiedPeriodTemperature>currentPeriodTemperature)//on a trouvé une future augmentation de température
                {
                    double timeDifference=(double)studiedPeriodOffset-((double)nowInMinutes+(double)second/60d);//en minutes
                    double temperatureDifference=studiedPeriodTemperature-currentPeriodTemperature;
                    double timeNeededToAdapt=temperatureDifference/3d*60d;//en minutes
                    if(timeNeededToAdapt>=timeDifference)//cette augmentation de température est à anticiper dès maintenant
                    {
                        double ratio=getAdjustedRatio(1d-timeDifference/timeNeededToAdapt);
                        double newTemperature=currentPeriodTemperature*(1d-ratio)+studiedPeriodTemperature*ratio;
                        if(newTemperature>adjustedTemperature)//si cette anticipation est plus importante que les autres (ou si c'est la première), on la garde
                            adjustedTemperature=newTemperature;
                    }
                }
            }
            //recherche de précédentes baisses de température à amortir
            for(int i=periods.length+currentPeriodIndex-1;i>=0;i--)
            {
                boolean previousWeek=i<periods.length;
                SchedulePeriod studiedPeriod=periods[i%periods.length];
                int studiedPeriodOffset=studiedPeriod.getEndOffset()-(previousWeek?60*24*7:0);
                if(studiedPeriodOffset<nowInMinutes-7*60)//trop loin de 7h, on abandonne
                    break;
                double studiedPeriodTemperature=studiedPeriod.getTemperature();
                if(studiedPeriodTemperature>currentPeriodTemperature)//on a trouvé une précédente baisse de température (et non pas une augmentation)
                {
                    double timeDifference=((double)nowInMinutes+(double)second/60d)-(double)studiedPeriodOffset;//en minutes
                    double temperatureDifference=studiedPeriodTemperature-currentPeriodTemperature;
                    double timeNeededToAdapt=temperatureDifference/2d*60d;//en minutes
                    if(timeNeededToAdapt>=timeDifference)//cette baisse de température est encore à amortir
                    {
                        double ratio=getAdjustedRatio(1d-timeDifference/timeNeededToAdapt);
                        double newTemperature=studiedPeriodTemperature*ratio+currentPeriodTemperature*(1d-ratio);
                        if(newTemperature>adjustedTemperature)//si cette baisse amortie aboutit à une température plus élevée que les autres (ou que la température théorique si c'est la première), on la garde
                            adjustedTemperature=newTemperature;
                    }
                }
            }
            return adjustedTemperature;
        }
        catch(Exception e)
        {
            e.printStackTrace();
            Logger.LOGGER.error("Error when computing current set point ("+e.toString()+")");
            return 19d;
        }
    }

    private static synchronized void refreshSchedules()
    {
        try
        {
            String clientId="";
            String clientSecret="";
            String userName="";
            String password="";
            String homeId="";
            try(BufferedReader reader=new BufferedReader(new FileReader(new File("netatmo.ini"))))
            {
                clientId=reader.readLine();
                clientSecret=reader.readLine();
                userName=reader.readLine();
                password=reader.readLine();
                homeId=reader.readLine();
            }
            catch(IOException e)
            {
                Logger.LOGGER.error("Unable to read Netatmo access informations from the config file ("+e.toString()+")");
            }
            if(token==null)
            {
                token=Token.getToken(clientId,clientSecret,userName,password);
                if(token!=null)
                    Logger.LOGGER.info("Initial token successfully acquired");
                else
                    Logger.LOGGER.error("Unable to acquire initial token");
            }
            if(token!=null&&token.isExpired())
            {
                token=token.renewExpiredToken();
                if(token!=null)
                    Logger.LOGGER.info("Token successfully renewed");
                else
                    Logger.LOGGER.error("Unable to renew token");
            }
            JsonObject rootObject1;
            try
            {
                rootObject1=Token.doURLRequest("GET","https://api.netatmo.com/api/homestatus","access_token="+token.getAccessTokenString()+"&home_id="+homeId);
            }
            catch(JsonParseException e)
            {
                throw new NetatmoException("Unreadable response",e);
            }
            catch(IOException e)
            {
                if(e.getMessage().contains("Server returned HTTP response code: 403"))
                    throw new TokenExpiredException();
                else
                    throw new NetatmoException("Unknown error during request",e);
            }
            JsonObject rootObject2;
            try
            {
                rootObject2=Token.doURLRequest("GET","https://api.netatmo.com/api/homesdata","access_token="+token.getAccessTokenString()+"&home_id="+homeId);
            }
            catch(JsonParseException e)
            {
                throw new NetatmoException("Unreadable response",e);
            }
            catch(IOException e)
            {
                if(e.getMessage().contains("Server returned HTTP response code: 403"))
                    throw new TokenExpiredException();
                else
                    throw new NetatmoException("Unknown error during request",e);
            }
            try
            {
                JsonObject bodyObject=rootObject1.get("body").getAsJsonObject();
                JsonObject homeObject=bodyObject.get("home").getAsJsonObject();
                JsonArray roomsArray=homeObject.get("rooms").getAsJsonArray();
                JsonObject room0Object=roomsArray.get(0).getAsJsonObject();
                String oldStatusMode=statusMode;
                statusMode=room0Object.get("therm_setpoint_mode").getAsString();
                double oldManualSetPoint=manualSetPoint;
                if(statusMode.equals("manual"))
                    manualSetPoint=room0Object.get("therm_setpoint_temperature").getAsDouble();
                if(oldStatusMode.equals(statusMode))
                    Logger.LOGGER.info("The status mode is always "+statusMode);
                else
                    Logger.LOGGER.info("Now the status mode is "+statusMode+" instead of "+oldStatusMode);
                if(statusMode.equals("manual"))
                    if(oldManualSetPoint==manualSetPoint)
                        Logger.LOGGER.info("The manual set point is always "+manualSetPoint+"°C");
                    else
                        Logger.LOGGER.info("Now the manual set point is "+manualSetPoint+"°C instead of "+oldManualSetPoint+"°C");
                bodyObject=rootObject2.get("body").getAsJsonObject();
                JsonArray homesArray=bodyObject.get("homes").getAsJsonArray();
                JsonObject home0Object=homesArray.get(0).getAsJsonObject();
                String oldDataMode=dataMode;
                dataMode=home0Object.get("therm_mode").getAsString();
                if(oldDataMode.equals(dataMode))
                    Logger.LOGGER.info("The data mode is always "+dataMode);
                else
                    Logger.LOGGER.info("Now the data mode is "+dataMode+" instead of "+oldDataMode);
                JsonArray schedulesArray=home0Object.get("schedules").getAsJsonArray();
                SCHEDULES.clear();
                for(int i=0;i<schedulesArray.size();i++)
                {
                    Schedule schedule=new Schedule();
                    JsonObject scheduleObject=schedulesArray.get(i).getAsJsonObject();
                    schedule.setId(scheduleObject.get("id").getAsString());
                    schedule.setName(scheduleObject.get("name").getAsString());
                    schedule.setAway_temp(scheduleObject.get("away_temp").getAsDouble());
                    schedule.setHg_temp(scheduleObject.get("hg_temp").getAsDouble());
                    schedule.setSelected(scheduleObject.has("selected"));
                    JsonArray zonesArray=scheduleObject.get("zones").getAsJsonArray();
                    for(int j=0;j<zonesArray.size();j++)
                    {
                        ScheduleZone zone=new ScheduleZone();
                        JsonObject zoneObject=zonesArray.get(j).getAsJsonObject();
                        zone.setId(zoneObject.get("id").getAsInt());
                        zone.setName(zoneObject.get("name").getAsString());
                        zone.setType(zoneObject.get("type").getAsInt());
                        zone.setTemperature(zoneObject.get("rooms").getAsJsonArray().get(0).getAsJsonObject().get("therm_setpoint_temperature").getAsDouble());
                        schedule.getZones().add(zone);
                    }
                    JsonArray timesArray=scheduleObject.get("timetable").getAsJsonArray();
                    for(int j=0;j<timesArray.size();j++)
                    {
                        ScheduleTime time=new ScheduleTime();
                        JsonObject timeObject=timesArray.get(j).getAsJsonObject();
                        time.setZone_id(timeObject.get("zone_id").getAsInt());
                        time.setM_offset(timeObject.get("m_offset").getAsInt());
                        schedule.getTimes().add(time);
                    }
                    SCHEDULES.add(schedule);
                }
                Logger.LOGGER.info(SCHEDULES.size()+" schedules loaded successfully, default one is "+SCHEDULES.stream().filter(Schedule::isSelected).findFirst().orElse(SCHEDULES.get(0)).getName());
            }
            catch(JsonParseException e)
            {
                throw new NetatmoException("Unreadable response",e);
            }
        }
        catch(Exception e)
        {
            Logger.LOGGER.error("Unable to update infos from Netatmo thermostat ("+e.toString()+")");
        }
    }

    private static double getAdjustedRatio(double ratio)
    {
        return Math.min(1d,Math.max(0d,(Math.sin(ratio*Math.PI-Math.PI/2d)+1d)/2d));
    }

    private static double getZoneTemperature(Schedule schedule,int zone_id)
    {
        ScheduleZone currentZone=null;
        for(ScheduleZone zone:schedule.getZones())
            if(zone.getId()==zone_id)
            {
                currentZone=zone;
                break;
            }
        if(currentZone==null)
            currentZone=schedule.getZones().get(0);
        return currentZone.getTemperature();
    }

    public static synchronized void setOverridingSetPoint(double setPoint,double hours)
    {
        overridingSetPointEnabled=true;
        overridingSetPoint=setPoint;
        overridingSetPointStartTime=System.currentTimeMillis();
        overridingSetPointHours=hours;
    }

    public static synchronized void resetOverridingSetPoint()
    {
        overridingSetPointEnabled=false;
        overridingSetPoint=0d;
        overridingSetPointStartTime=0L;
        overridingSetPointHours=0d;
    }
}

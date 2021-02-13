package sky.toilettes;

import sky.housecommon.PricingPeriod;

public class InstantaneousConsumption
{
    private final long time;
    private final PricingPeriod pricingPeriod;
    private final double blueDayOffPeakHourTotal;
    private final double blueDayPeakHourTotal;
    private final double whiteDayOffPeakHourTotal;
    private final double whiteDayPeakHourTotal;
    private final double redDayOffPeakHourTotal;
    private final double redDayPeakHourTotal;
    private final String consumer1Name;
    private final int consumer1Consumption;
    private final String consumer2Name;
    private final int consumer2Consumption;
    private final String consumer3Name;
    private final int consumer3Consumption;
    private final String consumer4Name;
    private final int consumer4Consumption;
    private final String consumer5Name;
    private final int consumer5Consumption;
    private final String consumer6Name;
    private final int consumer6Consumption;
    private final String consumer7Name;
    private final int consumer7Consumption;
    private final String consumer8Name;
    private final int consumer8Consumption;
    private final String consumer9Name;
    private final int consumer9Consumption;
    private final String consumer10Name;
    private final int consumer10Consumption;

    public InstantaneousConsumption(long time,PricingPeriod pricingPeriod,double blueDayOffPeakHourTotal,double blueDayPeakHourTotal,double whiteDayOffPeakHourTotal,double whiteDayPeakHourTotal,double redDayOffPeakHourTotal,double redDayPeakHourTotal,String consumer1Name,int consumer1Consumption,String consumer2Name,int consumer2Consumption,String consumer3Name,int consumer3Consumption,String consumer4Name,int consumer4Consumption,String consumer5Name,int consumer5Consumption,String consumer6Name,int consumer6Consumption,String consumer7Name,int consumer7Consumption,String consumer8Name,int consumer8Consumption,String consumer9Name,int consumer9Consumption,String consumer10Name,int consumer10Consumption)
    {
        this.time=time;
        this.pricingPeriod=pricingPeriod;
        this.blueDayOffPeakHourTotal=blueDayOffPeakHourTotal;
        this.blueDayPeakHourTotal=blueDayPeakHourTotal;
        this.whiteDayOffPeakHourTotal=whiteDayOffPeakHourTotal;
        this.whiteDayPeakHourTotal=whiteDayPeakHourTotal;
        this.redDayOffPeakHourTotal=redDayOffPeakHourTotal;
        this.redDayPeakHourTotal=redDayPeakHourTotal;
        this.consumer1Name=consumer1Name;
        this.consumer1Consumption=consumer1Consumption;
        this.consumer2Name=consumer2Name;
        this.consumer2Consumption=consumer2Consumption;
        this.consumer3Name=consumer3Name;
        this.consumer3Consumption=consumer3Consumption;
        this.consumer4Name=consumer4Name;
        this.consumer4Consumption=consumer4Consumption;
        this.consumer5Name=consumer5Name;
        this.consumer5Consumption=consumer5Consumption;
        this.consumer6Name=consumer6Name;
        this.consumer6Consumption=consumer6Consumption;
        this.consumer7Name=consumer7Name;
        this.consumer7Consumption=consumer7Consumption;
        this.consumer8Name=consumer8Name;
        this.consumer8Consumption=consumer8Consumption;
        this.consumer9Name=consumer9Name;
        this.consumer9Consumption=consumer9Consumption;
        this.consumer10Name=consumer10Name;
        this.consumer10Consumption=consumer10Consumption;
    }

    public long getTime()
    {
        return time;
    }

    public PricingPeriod getPricingPeriod()
    {
        return pricingPeriod;
    }

    public double getBlueDayOffPeakHourTotal()
    {
        return blueDayOffPeakHourTotal;
    }

    public double getBlueDayPeakHourTotal()
    {
        return blueDayPeakHourTotal;
    }

    public double getWhiteDayOffPeakHourTotal()
    {
        return whiteDayOffPeakHourTotal;
    }

    public double getWhiteDayPeakHourTotal()
    {
        return whiteDayPeakHourTotal;
    }

    public double getRedDayOffPeakHourTotal()
    {
        return redDayOffPeakHourTotal;
    }

    public double getRedDayPeakHourTotal()
    {
        return redDayPeakHourTotal;
    }

    public double getTotal(PricingPeriod aPricingPeriod)
    {
        return aPricingPeriod.getTotal(this);
    }

    public double getTotalOfTotals()
    {
        int total=0;
        for(PricingPeriod aPricingPeriod:PricingPeriod.values())
            total+=(int)(getTotal(aPricingPeriod)*1000d);
        return (double)total/1000d;//le passage par des int évite les erreurs d'arrondis en double
    }

    public String getConsumer1Name()
    {
        return consumer1Name;
    }

    public int getConsumer1Consumption()
    {
        return consumer1Consumption;
    }

    public String getConsumer2Name()
    {
        return consumer2Name;
    }

    public int getConsumer2Consumption()
    {
        return consumer2Consumption;
    }

    public String getConsumer3Name()
    {
        return consumer3Name;
    }

    public int getConsumer3Consumption()
    {
        return consumer3Consumption;
    }

    public String getConsumer4Name()
    {
        return consumer4Name;
    }

    public int getConsumer4Consumption()
    {
        return consumer4Consumption;
    }

    public String getConsumer5Name()
    {
        return consumer5Name;
    }

    public int getConsumer5Consumption()
    {
        return consumer5Consumption;
    }

    public String getConsumer6Name()
    {
        return consumer6Name;
    }

    public int getConsumer6Consumption()
    {
        return consumer6Consumption;
    }

    public String getConsumer7Name()
    {
        return consumer7Name;
    }

    public int getConsumer7Consumption()
    {
        return consumer7Consumption;
    }

    public String getConsumer8Name()
    {
        return consumer8Name;
    }

    public int getConsumer8Consumption()
    {
        return consumer8Consumption;
    }

    public String getConsumer9Name()
    {
        return consumer9Name;
    }

    public int getConsumer9Consumption()
    {
        return consumer9Consumption;
    }

    public String getConsumer10Name()
    {
        return consumer10Name;
    }

    public int getConsumer10Consumption()
    {
        return consumer10Consumption;
    }

    public String getConsumerName(int rank)
    {
        try
        {
            return (String)getClass().getMethod("getConsumer"+rank+"Name").invoke(this);
        }
        catch(Exception e)
        {
            return null;
        }
    }

    public int getConsumerConsumption(int rank)
    {
        try
        {
            return ((Integer)getClass().getMethod("getConsumer"+rank+"Consumption").invoke(this)).intValue();
        }
        catch(Exception e)
        {
            return -1;
        }
    }

    public int getTotalOfConsumptions()
    {
        int total=0;
        for(int rank=1;rank<=10;rank++)
            total+=getConsumerConsumption(rank);
        return total;
    }

    @Override
    public String toString()
    {
        return getClass().getSimpleName()
               + "\r\n{"
               + "\r\n\ttime="+time+","
               + "\r\n\tpricingPeriod="+pricingPeriod+","
               + "\r\n\tblueDayOffPeakHourTotal="+blueDayOffPeakHourTotal+" kWh,"
               + "\r\n\tblueDayPeakHourTotal="+blueDayPeakHourTotal+" kWh,"
               + "\r\n\twhiteDayOffPeakHourTotal="+whiteDayOffPeakHourTotal+" kWh,"
               + "\r\n\twhiteDayPeakHourTotal="+whiteDayPeakHourTotal+" kWh,"
               + "\r\n\tredDayOffPeakHourTotal="+redDayOffPeakHourTotal+" kWh,"
               + "\r\n\tredDayPeakHourTotal="+redDayPeakHourTotal+" kWh,"
               + "\r\n\ttotalOfTotals="+getTotalOfTotals()+" kWh,"
               + "\r\n\t\""+consumer1Name+"\"="+consumer1Consumption+" W,"
               + "\r\n\t\""+consumer2Name+"\"="+consumer2Consumption+" W,"
               + "\r\n\t\""+consumer3Name+"\"="+consumer3Consumption+" W,"
               + "\r\n\t\""+consumer4Name+"\"="+consumer4Consumption+" W,"
               + "\r\n\t\""+consumer5Name+"\"="+consumer5Consumption+" W,"
               + "\r\n\t\""+consumer6Name+"\"="+consumer6Consumption+" W,"
               + "\r\n\t\""+consumer7Name+"\"="+consumer7Consumption+" W,"
               + "\r\n\t\""+consumer8Name+"\"="+consumer8Consumption+" W,"
               + "\r\n\t\""+consumer9Name+"\"="+consumer9Consumption+" W,"
               + "\r\n\t\""+consumer10Name+"\"="+consumer10Consumption+" W,"
               + "\r\n\ttotalOfConsumptions="+getTotalOfConsumptions()+" W"
               + "\r\n}";
    }
}

package sky.toilettes;

public class Temperature//TODO à mettre dans la librairie commune
{
    private final long time;
    private final double temperature;
    private final double setPoint;
    private final double ratio;
    private final boolean heaterOn;

    public Temperature(long time,double temperature,double setPoint,double ratio,boolean heaterOn)
    {
        this.time=time;
        this.temperature=temperature;
        this.setPoint=setPoint;
        this.ratio=ratio;
        this.heaterOn=heaterOn;
    }

    public long getTime()
    {
        return time;
    }

    public double getTemperature()
    {
        return temperature;
    }

    public double getSetPoint()
    {
        return setPoint;
    }

    public double getRatio()
    {
        return ratio;
    }

    public boolean isHeaterOn()
    {
        return heaterOn;
    }
}

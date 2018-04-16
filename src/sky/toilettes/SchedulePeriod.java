package sky.toilettes;

public class SchedulePeriod
{
    private int startOffset;
    private int endOffset;
    private double temperature;

    public int getStartOffset()
    {
        return startOffset;
    }

    public void setStartOffset(int startOffset)
    {
        this.startOffset=startOffset;
    }

    public int getEndOffset()
    {
        return endOffset;
    }

    public void setEndOffset(int endOffset)
    {
        this.endOffset=endOffset;
    }

    public double getTemperature()
    {
        return temperature;
    }

    public void setTemperature(double temperature)
    {
        this.temperature=temperature;
    }
}

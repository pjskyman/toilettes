package sky.toilettes;

public class Time
{
    private final int value;
    private final long previousValue;

    private Time(int value)
    {
        this(value,0L);
    }

    private Time(int value,long previousValue)
    {
        this.value=value;
        this.previousValue=previousValue;
    }

    private long millisecondImpl()
    {
        return (long)value;
    }

    public long millisecond()
    {
        return millisecondImpl()+previousValue;
    }

    public Time millisecondPlus(int value)
    {
        return new Time(value,millisecond());
    }

    public Time millisecondMinus(int value)
    {
        return new Time(-value,millisecond());
    }

    private long secondImpl()
    {
        return 1000L*millisecondImpl();
    }

    public long second()
    {
        return secondImpl()+previousValue;
    }

    public Time secondPlus(int value)
    {
        return new Time(value,second());
    }

    public Time secondMinus(int value)
    {
        return new Time(-value,second());
    }

    private long minuteImpl()
    {
        return 60L*secondImpl();
    }

    public long minute()
    {
        return minuteImpl()+previousValue;
    }

    public Time minutePlus(int value)
    {
        return new Time(value,minute());
    }

    public Time minuteMinus(int value)
    {
        return new Time(-value,minute());
    }

    private long hourImpl()
    {
        return 60L*minuteImpl();
    }

    public long hour()
    {
        return hourImpl()+previousValue;
    }

    public Time hourPlus(int value)
    {
        return new Time(value,hour());
    }

    public Time hourMinus(int value)
    {
        return new Time(-value,hour());
    }

    private long dayImpl()
    {
        return 24L*hourImpl();
    }

    public long day()
    {
        return dayImpl()+previousValue;
    }

    public Time dayPlus(int value)
    {
        return new Time(value,day());
    }

    public Time dayMinus(int value)
    {
        return new Time(-value,day());
    }

    private long weekImpl()
    {
        return 7L*dayImpl();
    }

    public long week()
    {
        return weekImpl()+previousValue;
    }

    public Time weekPlus(int value)
    {
        return new Time(value,week());
    }

    public Time weekMinus(int value)
    {
        return new Time(-value,week());
    }

    public static Time get(int value)
    {
        return new Time(value);
    }

    public static void main(String[] args)
    {
        System.out.println(Time.get(1).millisecond());
        System.out.println(Time.get(1).second());
        System.out.println(Time.get(1).minute());
        System.out.println(Time.get(1).hour());
        System.out.println(Time.get(1).day());
        System.out.println(Time.get(1).week());
        System.out.println(Time.get(1).weekPlus(2).minutePlus(3).secondMinus(7).millisecond());
    }
}

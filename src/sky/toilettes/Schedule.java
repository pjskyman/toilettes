package sky.toilettes;

import java.util.ArrayList;
import java.util.List;

public class Schedule
{
    private String id;
    private String name;
    private boolean selected;
    private double away_temp;
    private double hg_temp;
    private final List<ScheduleZone> zones=new ArrayList<>();
    private final List<ScheduleTime> times=new ArrayList<>();

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id=id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name=name;
    }

    public boolean isSelected()
    {
        return selected;
    }

    public void setSelected(boolean selected)
    {
        this.selected=selected;
    }

    public double getAway_temp()
    {
        return away_temp;
    }

    public void setAway_temp(double away_temp)
    {
        this.away_temp=away_temp;
    }

    public double getHg_temp()
    {
        return hg_temp;
    }

    public void setHg_temp(double hg_temp)
    {
        this.hg_temp=hg_temp;
    }

    public List<ScheduleZone> getZones()
    {
        return zones;
    }

    public List<ScheduleTime> getTimes()
    {
        return times;
    }
}

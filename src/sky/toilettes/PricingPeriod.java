package sky.toilettes;

public enum PricingPeriod
{
    UNKNOWN
    {
        public int getCode()
        {
            return 0;
        }

        public double getTotal(InstantaneousConsumption instantaneousConsumption)
        {
            return 0d;
        }

        @Override
        public String toString()
        {
            return "Inconnu";
        }
    },
    BLUE_DAY_OFF_PEAK_HOUR
    {
        public int getCode()
        {
            return 5;
        }

        public double getTotal(InstantaneousConsumption instantaneousConsumption)
        {
            return instantaneousConsumption.getBlueDayOffPeakHourTotal();
        }

        @Override
        public String toString()
        {
            return "Bleu HC";
        }
    },
    BLUE_DAY_PEAK_HOUR
    {
        public int getCode()
        {
            return 8;
        }

        public double getTotal(InstantaneousConsumption instantaneousConsumption)
        {
            return instantaneousConsumption.getBlueDayPeakHourTotal();
        }

        @Override
        public String toString()
        {
            return "Bleu HP";
        }
    },
    WHITE_DAY_OFF_PEAK_HOUR
    {
        public int getCode()
        {
            return 6;
        }

        public double getTotal(InstantaneousConsumption instantaneousConsumption)
        {
            return instantaneousConsumption.getWhiteDayOffPeakHourTotal();
        }

        @Override
        public String toString()
        {
            return "Blanc HC";
        }
    },
    WHITE_DAY_PEAK_HOUR
    {
        public int getCode()
        {
            return 9;
        }

        public double getTotal(InstantaneousConsumption instantaneousConsumption)
        {
            return instantaneousConsumption.getWhiteDayPeakHourTotal();
        }

        @Override
        public String toString()
        {
            return "Blanc HP";
        }
    },
    RED_DAY_OFF_PEAK_HOUR
    {
        public int getCode()
        {
            return 7;
        }

        public double getTotal(InstantaneousConsumption instantaneousConsumption)
        {
            return instantaneousConsumption.getRedDayOffPeakHourTotal();
        }

        @Override
        public String toString()
        {
            return "Rouge HC";
        }
    },
    RED_DAY_PEAK_HOUR
    {
        public int getCode()
        {
            return 10;
        }

        public double getTotal(InstantaneousConsumption instantaneousConsumption)
        {
            return instantaneousConsumption.getRedDayPeakHourTotal();
        }

        @Override
        public String toString()
        {
            return "Rouge HP";
        }
    },
    ;

    public boolean isBlueDay()
    {
        return name().contains("BLUE_DAY");
    }

    public boolean isWhiteDay()
    {
        return name().contains("WHITE_DAY");
    }

    public boolean isRedDay()
    {
        return name().contains("RED_DAY");
    }

    public boolean isOffPeakHourPeriod()
    {
        return name().contains("OFF_PEAK_HOUR");
    }

    public boolean isPeakHourPeriod()
    {
        return !isOffPeakHourPeriod()&&this!=UNKNOWN;
    }

    public abstract int getCode();

    public abstract double getTotal(InstantaneousConsumption instantaneousConsumption);

    public static PricingPeriod getPricingPeriodForCode(int code)
    {
        for(PricingPeriod pricingPeriod:values())
            if(pricingPeriod.getCode()==code)
                return pricingPeriod;
        return UNKNOWN;
    }
}
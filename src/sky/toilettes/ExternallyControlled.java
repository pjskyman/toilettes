package sky.toilettes;

public interface ExternallyControlled
{
    public void on();

    public void off();

    public void set(double setPoint,double hours);

    public void reset();
}

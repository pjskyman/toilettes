package sky.toilettes;

public class NotAvailableDatabaseException extends Exception
{
    public NotAvailableDatabaseException()
    {
    }

    public NotAvailableDatabaseException(String message)
    {
        super(message);
    }

    public NotAvailableDatabaseException(String message,Throwable cause)
    {
        super(message,cause);
    }

    public NotAvailableDatabaseException(Throwable cause)
    {
        super(cause);
    }
}

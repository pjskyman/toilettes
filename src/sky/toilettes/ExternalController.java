package sky.toilettes;

import java.net.ServerSocket;

public class ExternalController
{
    private final ServerSocket serverSocket;
    private boolean active;
    private static final int SERVER_SOCKET_PORT=65432;

    public ExternalController(ExternallyControlled externallyControlled)
    {
        ServerSocket tempServerSocket;
        try
        {
            tempServerSocket=new ServerSocket(SERVER_SOCKET_PORT);
        }
        catch(Exception e)
        {
            Logger.LOGGER.error("Error when creating the external controller server socket on port "+SERVER_SOCKET_PORT+": "+e.toString());
            tempServerSocket=null;
        }
        serverSocket=tempServerSocket;
        active=true;
        new Thread()
        {
            @Override
            public void run()
            {
                while(active)
                    try
                    {
                        new Thread(new ExternalControllerClient(externallyControlled,serverSocket.accept())).start();
                    }
                    catch(Exception e)
                    {
                        Logger.LOGGER.error("Error when waiting for client connection: "+e.toString());
                    }
            }
        }.start();
    }

    public void dispose()
    {
        active=false;
    }
}

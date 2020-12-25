package src;

import java.io.File;
import java.io.FileWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Formatter;
import java.util.LinkedList;
import java.util.Queue;

public class Log extends Thread
{
    private Queue<String> coda;
    private String filename;
    private FileWriter fw;
    private Formatter writer;
    
    public Log(String filename)
    {
        this.filename = "../lop/" + filename;
        this.coda = new LinkedList<>();
        try
        {
            this.fw = new FileWriter(this.filename, true);
            this.writer = new Formatter(this.fw);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public synchronized void add_msg(String msg)
    {
        if (msg != null)
        {
            this.coda.add(msg);
        }
    }

    public void writeToFile(String msg)
    {
        try
        {
            LocalDateTime myDateObj = LocalDateTime.now();
            DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
            String formattedDate = myDateObj.format(myFormatObj);

            if (msg != null)
            {
                this.writer.format("[ %s ] -- %s\n", formattedDate, msg);
                this.writer.flush();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    @Override
    public void run()
    {
        try
        {
            File lop_dir = new File("../lop");
            if (!lop_dir.exists())
            {
                lop_dir.mkdirs();
            }
            
            while (!Thread.currentThread().isInterrupted())
            {
                this.writeToFile(this.coda.poll());
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    public void shutdown ()
    {
        if (!this.coda.isEmpty())
        {
            try
            {
                for (String msg : this.coda)
                {
                    this.writeToFile(msg);
                }

                this.writer.close();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
    }
}
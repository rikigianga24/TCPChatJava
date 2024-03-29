package src;

import com.google.common.base.Charsets;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import java.net.InetAddress;
import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.util.Scanner;
import java.util.Stack;
import org.json.JSONObject;

public class Console 
{
    private Stack<String> history = new Stack<>();

    public void avvia()
    {
        DatabaseTable utenti = new DatabaseTable("utenti", "username");
        DatabaseTable messaggi = new DatabaseTable("messaggi", "id");

        String command = "";

        Scanner input = new Scanner(System.in);

        // Finché il comando non e' null processa il comando letto
        while (command != null)
        {
            System.out.print("? ");
            command = input.nextLine();
            String[] arguments = command.split(" ");
            String com = arguments[0];

            Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " ricevuto comando: " + command);

            switch (com)
            {
                case "history":
                    int i = 0;
                    for (String c : history)
                    {
                        System.out.println(i++ + ": " + c);
                    }
                break;
                case "s":
                case "stop":
                case "e":
                case "exit":
                    command = null;
                break;
                case "ban":
                    String ip = arguments[1].split("\\/")[0];
                    Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " banna: " + ip);
                    try
                    {
                        Server.getServer().ban(InetAddress.getByName(ip));
                    }
                    catch (Exception e)
                    {
                        Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e);
                    }
                break;
                case "numero-errori":
                case "n-err":
                    System.out.println(Server.getServer().numeroErrori());
                break;
                case "error":
                case "err":
                    String nErr = (Server.getServer().numeroErrori() == 1)
                        ? "( 1 Errore ) - "
                        : "( " + Server.getServer().numeroErrori() + " Errori ) - ";

                    try
                    {
                        int index = Integer.parseInt(arguments[1]);

                        String err = Server.getServer().erroreNellIndex(index);

                        if (err == null)
                        {
                            System.out.println(nErr + "Errore n. " + index + " non trovato");
                        }
                        else if (err.equals(""))
                        {
                            System.out.println("Ancora non ci sono errori!");
                        }
                        else
                        {
                            System.out.println(nErr + "Errore n. " + index + ": " + err);
                        }
                    }
                    catch (IndexOutOfBoundsException e)
                    {
                        String err = Server.getServer().ultimoErrore();

                        if (err.equals(""))
                        {
                            System.out.println("Ancora non ci sono errori!");
                        }
                        else
                        {
                            System.out.println(nErr + "Ultimo errore: " + err);
                        }
                        
                    }
                break;
                case "del-user":
                case "du":
                    String u = arguments[1];

                    try
                    {
                        utenti.deleteOnce(u);
                    }
                    catch (Exception e)
                    {
                        Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e);
                    }
                break;
                case "svuota-db-messaggi":
                case "svuota-db-msg":
                case "sdbm":
                    try
                    {
                        messaggi.delete();
                    }
                    catch (Exception e)
                    {
                        Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e);
                    }
                break;
                case "show-connected-client":
                case "showcc":
                case "scc":
                    try
                    {
                        Object[] clients = Server.getServer().getConnectedClients().keySet().toArray();
                        if (clients.length == 1)
                        {
                            System.out.println("C'e' un solo client connesso");
                        }
                        else
                        {
                            System.out.println("Ci sono " + clients.length + " client connessi");
                        }

                        System.out.println("******************************************************");

                        for (int j = 0; j < clients.length; ++j)
                        {
                            Client c = (Client)clients[j];
                            System.out.println("> " + c.getAddress() + " => " + c.getNome());
                        }
                    }
                    catch (Exception e)
                    {
                        Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e);
                    }
                break;
                case "utenti-registrati":
                case "ur":
                    try
                    {
                        ResultSet result = utenti.findAll();

                        int j = 0;
                        while (result.next())
                        {
                            System.out.println(j++ + ": " + result.getString("username"));
                        }
                    }
                    catch (Exception e)
                    {
                        Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e);
                    }
                break;
                case "aggiungi-utente":
                case "a-utente":
                case "au":
                    try
                    {
                        String n = arguments[1];
                        String p = arguments[2];

                        HashCode h = Hashing.sha256().hashString(p, Charsets.UTF_8);
                        String hash = h.toString();

                        String[] fields = new String[2];
                        fields[0] = "username";
                        fields[1] = "password";

                        String[] values = new String[2];
                        values[0] = n;
                        values[1] = hash;

                        utenti.insertOnce(fields, values);
                    }
                    catch (Exception e)
                    {
                        Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e);
                    }
                break;
                case "manda-msg":
                case "send-msg":
                    String msg = "";

                    for (int j = 1; j < arguments.length; ++j)
                    {
                        msg += arguments[j] + " ";
                    }

                    if (msg.length() >= 256)
                    {
                        System.out.println("Messaggio troppo lungo (max. 256 caratteri).");
                        System.out.println("Il messaggio non verra' inviato.");
                        break;
                    }

                    if (msg != null && msg.length() != 0)
                    {
                        JSONObject json = new JSONObject();
                        json.put("Tipo-Richiesta", "Nuovo-Messaggio");
                        json.put("Tipo-Messaggio", "Plain-Text");
                        json.put("Nome", "SERVER");
                        json.put("Messaggio", msg);

                        Server.getServer().messaggioBroadcast(json.toString());
                    }
                break;
                case "n-message-by":
                case "n-msg-by":
                case "nmb":
                    String n = arguments[1];
                    
                    try
                    {
                        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " mi connetto al database");
                            Connection connection = DatabaseConnection.getConnection();
                        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " mi connetto al database");
                        
                        String query = "SELECT COUNT(*) as numero_messaggi "
                            + "FROM messaggi "
                            + "WHERE user = '" + n + "' AND "
                            + "date = CURRENT_DATE()";

                        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " creo un oggetto di tipo Statement");
                            Statement stmt = connection.createStatement();
                        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " oggetto creato");

                        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " eseguo la query per sapere quanti messaggi " + n + " ha mandato oggi");
                            ResultSet result = stmt.executeQuery(query);
                        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " query eseguita correttamente");
                    
                        int nMsg = 0;
                        if (result.next() && (nMsg = result.getInt("numero_messaggi")) != 0)
                        {
                            System.out.println("Oggi " + n + " ha mandato " + nMsg + " messaggi");
                        }
                        else
                        {
                            System.out.println("Oggi " + n + " non ha mandato nessun messaggio");
                        }
                    }
                    catch (Exception e)
                    {
                        Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e);
                    }
                break;
                default:
                    System.out.println("Comando non riconosciuto.");
            }

            history.push(command);
        }

        input.close();

        Server.getServer().logger.add_msg("[ OK  ] - Libero la memoria creata per contenere la history");

        history.clear();
    }
}
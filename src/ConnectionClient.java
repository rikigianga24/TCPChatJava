package src;

import org.json.JSONObject;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;

public class ConnectionClient implements Runnable
{
    private Socket socket;
    private Client client;

    private final int GRANDEZZA_BUFFER = 8192;

    private ArrayList<String> fragmentImg = new ArrayList<>();

    public ConnectionClient(Socket socket, Client client)
    {
        this.socket = socket;
        this.client = client;
    }

    @Override
    public void run() 
    {
        try
        {
            String msg = "";

            while (msg != null)
            {
                byte[] buffer = new byte[GRANDEZZA_BUFFER];
                int l = this.socket.getInputStream().read(buffer);
                msg = new String(buffer, 0, l, "UTF8");

                JSONObject richiesta = new JSONObject(msg);

                Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " gestisco tipo richiesta");
                switch (richiesta.getString("Tipo-Richiesta"))
                {
                    case "Autenticazione":
                        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " tipo richiesta: Autenticazione");
                        if (this.gestisciAutenticazione(richiesta))
                        {   
                            JSONObject autenticazioneCorretta = new JSONObject();
                            autenticazioneCorretta.put("Tipo-Richiesta", "Autenticazione");
                            autenticazioneCorretta.put("Utenti-Connessi", Server.getServer().getNumeroUtentiConnessi());
                            autenticazioneCorretta.put("Risultato", true);
                            
                            Server.getServer().mandaMessaggio(autenticazioneCorretta.toString(), null, this.socket);
                        
                            JSONObject numeroUtenti = new JSONObject();
                            numeroUtenti.put("Tipo-Richiesta", "Numero-Utenti");
                            numeroUtenti.put("Numero", Server.getServer().getNumeroUtentiConnessi());

                            Server.getServer().mandaMessaggio(numeroUtenti.toString(), this.client, null);
                        }
                        else
                        {
                            JSONObject autenticazioneCorretta = new JSONObject();
                            autenticazioneCorretta.put("Tipo-Richiesta", "Autenticazione");
                            autenticazioneCorretta.put("Risultato", false);
                            
                            Server.getServer().mandaMessaggio(autenticazioneCorretta.toString(), null, this.socket);
                            msg = null;
                        }
                    break;
                    case "Invio-Messaggio":
                        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " tipo richiesta: Invio-Messaggio");

                        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " controllo se il client è mutato o bannato");
                        if (Server.getServer().banned.contains(this.client.getAddress()))
                        {
                            JSONObject bannato = new JSONObject();
                            bannato.put("Tipo-Richiesta", "Non-Puoi-Inviare-Messaggi");
                            bannato.put("Motivo", "Sei stato bannato");

                            Server.getServer().mandaMessaggio(bannato.toString(), null, this.socket);

                            break;
                        }
                        else if (this.client.getCounter() == 0)
                        {
                            JSONObject mutato = new JSONObject();
                            mutato.put("Tipo-Richiesta", "Non-Puoi-Inviare-Messaggi");
                            mutato.put("Motivo", "Sei stato mutato per 1 minuto per aver mandato troppi messaggi");

                            Server.getServer().mandaMessaggio(mutato.toString(), null, this.socket);

                            break;
                        }

                        switch (richiesta.getString("Tipo-Messaggio"))
                        {
                            case "Immagine":
                                if (!richiesta.getBoolean("Fine"))
                                {
                                    fragmentImg.add(richiesta.getString("Messaggio"));
                                    break;
                                }

                                for (int i = 0; i < fragmentImg.size(); ++i)
                                {
                                    JSONObject invioMessaggio = new JSONObject(); 
                                    invioMessaggio.put("Tipo-Richiesta", "Nuovo-Messaggio");
                                    invioMessaggio.put("Tipo-Messaggio", richiesta.getString("Tipo-Messaggio"));
                                    invioMessaggio.put("Fine", false);
                                    invioMessaggio.put("Nome", this.client.getNome());
                                    invioMessaggio.put("Messaggio", fragmentImg.get(i));
    
                                    Server.getServer().mandaMessaggio(invioMessaggio.toString(), this.client, null);
    
                                    Thread.sleep(256);
                                }

                                JSONObject invioMessaggio = new JSONObject(); 
                                invioMessaggio.put("Tipo-Richiesta", "Nuovo-Messaggio");
                                invioMessaggio.put("Tipo-Messaggio", richiesta.getString("Tipo-Messaggio"));
                                invioMessaggio.put("Fine", true);
                                invioMessaggio.put("Nome", this.client.getNome());
                                invioMessaggio.put("Messaggio", "");
    
                                Server.getServer().mandaMessaggio(invioMessaggio.toString(), this.client, null);

                                Server.getServer().writer.addMsg(
                                    richiesta.getString("Data") + " " + richiesta.getString("Time")
                                    + "|" +
                                    richiesta.getString("Nome")
                                    + "|" +
                                    "Ha mandato un'immagine!"
                                );
                            break;
                            case "Plain-Text":
                                JSONObject invioPlainText = new JSONObject(); 
                                invioPlainText.put("Tipo-Richiesta", "Nuovo-Messaggio");
                                invioPlainText.put("Tipo-Messaggio", richiesta.getString("Tipo-Messaggio"));
                                invioPlainText.put("Nome", this.client.getNome());
                                invioPlainText.put("Messaggio", richiesta.getString("Messaggio"));

                                Server.getServer().mandaMessaggio(invioPlainText.toString(), this.client, null);

                                Server.getServer().writer.addMsg(
                                    richiesta.getString("Data") + " " + richiesta.getString("Time")
                                    + "|" +
                                    richiesta.getString("Nome")
                                    + "|" +
                                    richiesta.getString("Messaggio")
                                );
                            break;
                        }
                    break;
                    case "Chiudi-Connessione":
                        msg = null;
                    break;
                }
            }
        }
        catch (Exception e)
        {
            Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e);
        }

        try { this.socket.close(); } catch (Exception e) { Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e); }
        Server.getServer().rimuoviClient(this.client);

        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " " + this.client.getNome() + " si e' disconnesso, aggiorno il numero degli utenti connessi al client");
        JSONObject numeroUtenti = new JSONObject();
        numeroUtenti.put("Tipo-Richiesta", "Numero-Utenti");
        numeroUtenti.put("Numero", Server.getServer().getNumeroUtentiConnessi());
        Server.getServer().messaggioBroadcast(numeroUtenti.toString());
    }

    private boolean gestisciAutenticazione(JSONObject richiesta)
    {
        String nomeUtente = richiesta.getString("Nome");
        this.client.setNome(nomeUtente);
        Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " nuovo client: " + nomeUtente);

        try
        {
            Class.forName("com.mysql.cj.jdbc.Driver");

            Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " mi connetto al database e istanzio un oggetto di tipo Statement");

            Connection c = DatabaseConnection.getConnection();
            Statement s = c.createStatement();

            Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " connesso al database e creato oggetto Statement, ora eseguo la query di ricerca utete");

            ResultSet utenti = s.executeQuery("SELECT COUNT(*) as num_rows FROM utenti WHERE username = '" + nomeUtente + "'");

            Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " query eseguita correttamente");
            
            if (utenti.next() && utenti.getInt("num_rows") == 0)
            {
                Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " utente non riconosciuto (" + nomeUtente + ")");
                throw new Exception("Utente non riconosciuto");
            }

            Server.getServer().logger.add_msg("[ OK  ] - " + Thread.currentThread().getName() + " si e' connesso " + nomeUtente);

            return true;
        }
        catch (Exception e)
        {
            Server.getServer().logger.add_msg("[ ERR ] - " + Thread.currentThread().getName() + " " + e);
        }

        return false;
    }
}
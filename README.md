# TCP Server & Client
Piccolo progetto per una chat in java criptata.

## Dipendenze
- make
- mysql
- driver java per connettere il programma con mysql (su Linux: libmysql-java)

## Aggiungere il database
Per aggiungere il database basta scrivere:
```bash
make db
```

## Come compilare ed avviare il Server
Date i seguenti comandi nella directory del progetto:
```bash
make compile
make run-server
```

## Console del programma Server
Quando avviate un server, nella console apparirà:
```bash
?
```
Questo simbolo sta a significare che potete dare dei comandi al server che lui eseguirà.
I comandi per ora disponibili, con alias, sono:
- **exit** | **stop** (v. corta: *e*, *s*)
    - Interrompe il server
- **show-connected-client** (v. corta: *show-cc*, *scc*)
    - Stampa una lista dei client in questo momento connessi
- **history**
    - Stampa la lista dei comandi dati al server

## Come avviare il Client
Per avviare il client è sufficiente digitare:
```bash
cd out
java src.AppClient {indirizzo} {porta}
```

## Codice di errore Client
Se non riesci a connetterti con il client al server, puoi controllare
il codice di ritorno del client digitando (su Linux):
```bash
echo $?
```

Ecco una tabella dei codici di errore:
- 1 => **Connessione rifiutata**, il server potrebbe essere spento
- 2 => **IO Exception**, c'è stato un errore con il socket
- 3 => **Stream di output non inizializzato**, non è stato possibile inizializzare lo stream di output per mandare i messaggi

#### Thanks to: Daniele Castiglia, Francesco Borri, Sultan Zhunushov
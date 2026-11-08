# Progetto di Algoritmi e Strutture Dati 2024/25

Università degli Studi di Brescia, versione del progetto per gruppo di una persona.

**Studente:** Ergys Perdeda

Implementazione in Java dell'algoritmo CAMMINOMIN per il calcolo di cammini minimi su griglie 8-connesse con ostacoli.

## Relazione

- [Relazione_ASD_2024_25.pdf](relazione/Relazione_ASD_2024_25.pdf): relazione completa.
- [relazione/](relazione): sorgenti LaTeX (`Relazione_ASD_2024_25.tex` e una sezione per file in `sezioni/`).

## Risultati sperimentali

- [esperimenti.csv](risultati/esperimenti.csv): piano sperimentale, 67 istanze, una riga per istanza e variante di CamminoMin.
- [riassunti.txt](risultati/riassunti.txt): riassunto testuale di ogni riga del piano.
- [limiti_scala.csv](risultati/limiti_scala.csv): serie sui limiti di scala, 108 coppie per le varianti `base` e `limite-propagato`.
- [griglie_json/](risultati/griglie_json): le 23 griglie usate nel piano.
- [grafici/](risultati/grafici): grafici della relazione, generati da [genera_grafici.py](risultati/genera_grafici.py).

## Utilizzo

Richiede Java 17 o successivo. I comandi vanno eseguiti dalla radice della repository.

```text
java -jar asd-project.jar                          # piano sperimentale (circa un minuto)
java -jar asd-project.jar --limiti-scala           # serie sui limiti di scala, entrambe le varianti
java -jar asd-project.jar --limiti-scala base      # serie per una sola variante
java -jar asd-project.jar --help                   # opzioni per un'istanza singola
```

Compito Due su una griglia salvata: contesto, complemento e frontiera di O e distanza libera da D.

```text
java -jar asd-project.jar --grid-input risultati/griglie_json/scenario_02_12x12_15ost_seed50.json --origin 0,0 --destination 10,10 --chiusura
```

Compito Tre su una griglia generata o letta da file, con la variante scelta
(`base`, `senza-cache`, `senza-ordinamento`, `limite-propagato`):

```text
java -jar asd-project.jar --width 20 --height 20 --obstacles 40 --seed 77 --origin 0,0 --destination 19,19
java -jar asd-project.jar --grid-input griglia.json --origin 0,0 --destination 19,19 --variante limite-propagato
```

Compilazione ed esecuzione dei 68 test (Maven 3.9 o successivo):

```text
mvn -q clean package
```

Rigenerazione dei grafici (Python 3):

```text
python -m pip install -r requirements-report.txt
python risultati/genera_grafici.py
```

## Struttura del codice

- [mio/griglia](src/main/java/mio/griglia): griglia, celle e vista immutabile con ostacoli aggiuntivi.
- [mio/compito1](src/main/java/mio/compito1): **Compito Uno**, generatore casuale e persistenza JSON.
- [mio/compito2](src/main/java/mio/compito2): **Compito Due**, distanza libera, contesto, complemento e frontiera.
- [mio/compito3](src/main/java/mio/compito3): **Compito Tre**, algoritmo `CamminoMin`, sue varianti e sequenza dei landmark.
- [mio/compito4](src/main/java/mio/compito4): **Compito Quattro**, sperimentazione, riassunto, CSV e limiti di scala.
- [mio/Main.java](src/main/java/mio/Main.java): piano sperimentale e riga di comando.
- [src/test](src/test/java/mio): test JUnit.

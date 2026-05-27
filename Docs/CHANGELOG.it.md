# Changelog - Advanced Machinery

Tutte le modifiche rilevanti alla mod **Advanced Machinery** verranno documentate in questo file.

Il formato si basa su [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
e questo progetto aderisce al [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.9.0] - Pannello di Configurazione dei Lati I/O (Update 9)

### Aggiunto
- **Configuratore dei Lati I/O**: Un nuovo pannello laterale a comparsa nella GUI dell'Advanced Empowerer, accessibile tramite il pulsante "C".
- **Configurazione Relativa dei Lati**: Supporta la configurazione delle 6 facce del blocco relative al suo orientamento orizzontale: Alto (U), Basso (D), Fronte (F), Retro (B), Sinistra (L), Destra (R).
- **Cinque Modalità di Trasferimento**: Ciascun lato può alternare ciclicamente cinque stati, con codifica a colori:
  - **Disabilitato** (Grigio): Blocca completamente l'inserimento ed estrazione su quella faccia.
  - **Solo Inserimento Basi** (Blu): Consente solo l'inserimento di oggetti base nello slot 0.
  - **Solo Inserimento Modificatori** (Giallo): Consente solo l'inserimento di modificatori negli slot 1-4.
  - **Solo Estrazione Prodotto** (Verde): Consente solo l'estrazione dal solo slot di output 5.
  - **Qualsiasi** (Viola): Consente tutti gli inserimenti ed estrazioni standard.
- **Riposizionamento Pulsanti Automazione**: Spostati i pulsanti di configurazione Auto Input (I) e Auto Output (O) all'interno del nuovo pannello laterale a comparsa.
- **Wrapper Sided Item Handler Personalizzato**: Integrato `SidedItemHandlerWrapper` per instradare dinamicamente i tubi/hopper esterni a seconda della modalità impostata per ciascun lato.
- **Auto Push/Pull Condizionato**: Aggiornata la logica di auto-inserimento ed auto-estrazione a tick affinché rispetti le modalità configurate per i vari lati.
- **Supporto Multilingua**: Traduzioni per la configurazione dei lati e le modalità in inglese, italiano, tedesco, spagnolo, francese, portoghese, russo e cinese.

### Modificato
- **Riposizionamento Controlli GUI**: Rimossi i pulsanti `I` e `O` dalla GUI principale e spostati `R` (Round Robin) e `1` (Single Item Mode) a coordinate `y=24` per un design più pulito.
- **Espansione Simple Container Data**: Incrementato il numero di variabili sincronizzate da 12 a 18 slot per gestire lo stato dei 6 lati tra client e server.

---

## [0.8.1] - Ottimizzazione Ricettario e Traduzioni Complete

### Aggiunto
- **Caching delle Ricette**: Implementato un sistema di caching locale all'istanza della BlockEntity (`cachedRecipes`, `cachedBaseIngredients`, `cachedModifierIngredients`) per evitare costosi lookup del `RecipeManager` e iterazioni ripetute di stream ad ogni tick e inserimento.
- **Traduzione GUI Completa**: Aggiunte traduzioni complete in 8 lingue per le nuove opzioni della GUI e i dettagli del tooltip di telemetria dell'energia.

### Modificato
- **Localizzazione GUI**: Sostituiti tutti i testi letterali hardcodate con `Component.translatable(...)` per supportare la localizzazione del client.

---

## [0.8.0] - Automazione Avanzata e Telemetria GUI

### Aggiunto
- **Auto Input**: Permette di risucchiare automaticamente gli ingredienti dei crafting dai contenitori adiacenti.
- **Auto Output**: Espelle automaticamente i prodotti finiti dai contenitori adiacenti.
- **Round Robin**: Distribuisce equamente gli ingredienti modificatori in arrivo tra i 4 slot esterni.
- **Modalità Oggetto Singolo**: Limita la capienza degli slot di input (0-4) a 1 unità ciascuno (emulando i display stand).
- **Pulsanti di Controllo Automazione**: Aggiunti 4 pulsanti interattivi `I`, `O`, `R`, `1` nella GUI principale per attivare o disattivare le rispettive impostazioni.
- **Allineamento Automatico**: Gli ingredienti inseriti in disordine vengono scambiati di posto in automatico per posizionare l'oggetto base nello slot centrale 0.
- **Filtri di Inserimento Rigidi**: L'inventario della macchina e il sided handler accettano solo basi nello slot 0, modificatori negli slot 1-4, e upgrade nei slot 6-7, bloccando inserimenti casuali.
- **Telemetria Energetica Avanzata**: Visualizzazione nel tooltip dell'energia del costo della ricetta in FE, consumo in FE/t e dei bonus percentuali reali forniti dagli upgrade di velocità ed efficienza energetica.
- **ToggleAutoSettingPayload**: Pacchetto di rete per sincronizzare dal client al server i cambi di stato delle impostazioni.

### Modificato
- **`ContainerData` Espanso**: Incrementati gli slot di sincronizzazione da 6 a 12 indici per includere i flag di automazione e le metriche di consumo.
- **Wrapper Sided Capability**: Aggiornato `getSidedInventory` per ritornare il gestore di input dinamico centralizzato `externalItemHandler` con regole di inserimento rigide.

---

## [0.7.2] - Supporto Traduzioni e Configurazione Repository

### Aggiunto
- Traduzioni per tedesco (`de_de`), spagnolo (`es_es`), francese (`fr_fr`), portoghese (`pt_br`), russo (`ru_ru`) e cinese (`zh_cn`).
- Badge per il download da CurseForge e sezione compatibilità e note sui modpack nei file README in inglese e italiano.

### Modificato
- Configurazione per la normalizzazione dei fine riga nel file `.gitattributes`.
- Regole del file `.gitignore` aggiornate per migliorare la copertura dei file di build, artefatti IDE e ambienti di sviluppo.

---

## [0.7.1] - Allineamento GUI e Supporto Shader Energia

### Aggiunto
- **Texture Barra Energia Bianca**: Convertito lo sprite dell'energia a un colore interamente bianco per consentire la colorazione dinamica tramite shader (l'effetto arcobaleno di Actually Additions).

### Corretto
- **Dimensioni Barra Energia**: Ridotto il rettangolo di sfondo scuro della barra dell'energia da 90px a 85px nella texture della GUI per corrispondere esattamente alle dimensioni di Actually Additions.
- **Costante `ENERGY_HEIGHT`**: Aggiornata la costante `ENERGY_HEIGHT` da `90` a `83` in `AdvancedEmpowererScreen.java` per allineare l'altezza utile interna con quella di Actually Additions, prevenendo lo sforamento visivo a carica massima.
- **Coordinate Freccia di Progresso**: Aggiornate le coordinate e dimensioni della freccia di progresso in `AdvancedEmpowererScreen.java` per allinearsi al nuovo sprite `22x16`.

---

## [0.7.0] - Sistema Energy Upgrade e Fix Critici

### Aggiunto
- **Oggetto Energy Upgrade**: Sostituisce l'Efficiency Upgrade con un nuovo oggetto `Energy Upgrade` dal duplice scopo: riduce il consumo energetico per tick e aumenta la capacità del buffer energetico interno della macchina.
- **Buffer Energetico Dinamico**: La capacità energetica dell'Advanced Empowerer scala ora in modo esponenziale con gli Energy Upgrade installati, da 2.000.000 FE (base) fino a 20.000.000 FE (8 upgrade).

### Modificato
- **Riprogettazione Sistema Upgrade**: Lo slot upgrade precedentemente dedicato agli Efficiency Upgrade (slot 7) accetta ora gli Energy Upgrade, che combinano riduzione del consumo ed espansione del buffer in un unico oggetto.
- **Formula Velocità Esponenziale**: Lo Speed Upgrade usa ora `S(u) = 10^(u/8)`, raggiungendo 10x velocità con 8 upgrade (tempo ridotto da 200 a 20 tick).
- **Formula Energia Esponenziale**: L'energia per tick è ora `usage = baseUsage * 10^((2*S - E) / 8)`, permettendo agli Energy Upgrade di compensare completamente il costo aggiuntivo introdotto dagli Speed Upgrade.
- **Limite Slot Upgrade**: Entrambi gli slot Speed ed Energy Upgrade accettano ora fino a 8 oggetti (in precedenza 4).
- **`ContainerData` espanso a 6 valori**: Aggiunti due slot extra (indici 4 e 5) per sincronizzare il valore dinamico di `maxEnergy` al client, necessario per il nuovo buffer a capacità variabile.

### Corretto
- **`MutableEnergyStorage` dichiarata come inner class `static`**: In precedenza era una inner class non-statica e portava un riferimento implicito alla BlockEntity esterna, causando potential memory leak e un accoppiamento nascosto con l'handler dell'inventario. Ora è statica, con l'accesso agli upgrade delegato a un `IntSupplier` passato al costruttore.
- **`setStored()` con clamp corretto**: In precedenza assegnava il valore grezzo a `this.energy` senza controllo dei limiti. Se l'energia salvata nell'NBT superava la capacità attuale (es. dopo la rimozione di Energy Upgrade), `receiveEnergy()` ed `extractEnergy()` producevano risultati inconsistenti. Ora fa il clamp a `[0, getMaxEnergyStored()]`.
- **Sincronizzazione energia atomica (split a 16 bit)**: I due half-word di `energyStored` e `maxEnergy` vengono ora applicati atomicamente usando variabili di staging (`pendingEnergyLow`, `pendingMaxEnergyLow`). Il valore completo viene aggiornato solo quando arriva il half-word HI, evitando che la GUI legga un valore ibrido tra due frame di aggiornamento.
- **`ClientEvents` — aggiunto `bus = Bus.MOD`**: `RegisterMenuScreensEvent` è un Mod Bus event. Il parametro `bus` mancante faceva sì che fosse registrato sul Game Bus, quindi la schermata GUI non veniva mai registrata e il gioco andava in crash al primo clic destro sul blocco.
- **`neoforge.mods.toml` — corretta `versionRange` di NeoForge**: Era `[${neo_version},)` (espansa a `[21.1.223,)`), richiedendo esattamente quella build o superiore. Ora usa `${neo_version_range}` (definita come `[21.1.0,)` in `gradle.properties`), accettando qualsiasi build compatibile della family 21.1.x.

---

## [0.6.1] - Fix Coordinate Slot
### Corretto
- **Coordinate Slot**: Corrette le posizioni degli slot nella GUI per tutti gli slot di input e per lo slot di output, in modo che corrispondano correttamente al layout della texture definitiva.

---

## [0.6.0] - Finalizzazione GUI, Ricetta Crafting e JEI
### Aggiunto
- **Ricetta Crafting per Advanced Empowerer**: Aggiunta la ricetta di crafting in-game per ottenere il blocco Advanced Empowerer.
- **Texture GUI Definitiva**: Aggiunta la texture grafica definitiva per la schermata dell'Advanced Empowerer.
- **Registrazione come Catalizzatore JEI**: Il blocco Advanced Empowerer è ora registrato come catalizzatore di ricette in JEI.
- **Trasferimento Ricette JEI**: Supporto al trasferimento Shift+Click delle ricette da JEI alla GUI dell'Advanced Empowerer.

---

## [0.5.2] - Fix Range Slot JEI
### Corretto
- **Range Slot Trasferimento JEI**: Aggiornato il gestore di trasferimento ricette per fare riferimento correttamente a tutti gli 8 slot della macchina e ai 36 slot dell'inventario del giocatore, a seguito della modifica al layout a 5 slot di input introdotta nella 0.5.0.

---

## [0.5.1] - Pulizia Codice e Fix Minori
### Corretto
- **Controllo Null su `level`**: Aggiunto un controllo null per `level` nel metodo tick della block entity per prevenire potenziali NullPointerException.

### Modificato
- **Menu Client-Side Leggero**: Il costruttore lato client del menu usa ora un `ItemStackHandler` dummy leggero invece di un'istanza completa di `BlockEntity`, prevenendo crash in caso di race condition o chunk non ancora caricati. Il metodo `stillValid` restituisce ora `false` quando la block entity non è disponibile.

### Refactoring
- Pulizia di commenti e formattazione in `AdvancedEmpowererScreen`.
- Aggiornato il commento al drop dell'inventario in `AdvancedEmpowererBlock` per rispecchiare il range corretto degli slot.
- Affinati i commenti ai ruoli degli slot e rimossi appunti obsoleti in `AdvancedEmpowererBlockEntity`.

---

## [0.5.0] - Layout Input a 5 Slot e Refactor Asset
### Aggiunto
- **Supporto 5 Slot Input (BlockEntity)**: L'Advanced Empowerer supporta ora 1 slot base + 4 slot modifier, abilitando tutte le ricette standard dell'Empowerer di Actually Additions.
- **Supporto 5 Slot Input (Menu)**: Aggiornato il layout del container per rispecchiare il nuovo schema di input a croce con 5 slot (alto, sinistra, centro, destra, basso).
- **Varianti Direzionali Blockstate**: Aggiunte le varianti `facing` (nord, sud, est, ovest) alla definizione del blockstate dell'Advanced Empowerer.

### Modificato
- **Modello Blocco Ristrutturato**: Riorganizzati gli elementi e i gruppi del modello 3D del blocco per correttezza e coerenza visiva.

### Corretto
- **Fallback quickMoveStack**: Aggiunto un fallback per lo Shift+Click tra l'inventario principale e la hotbar del giocatore quando nessun slot della macchina accetta l'oggetto.

### Rimosso
- **Texture GUI Non Utilizzate**: Rimosse `advanced_empowerer_2.png` e `advanced_empowerer_3.png` (texture intermedie non più necessarie).

---

## [0.4.1] - Pulizia Build e Codice
### Modificato
- **Configurazione Build**: Aggiunta la proprietà `archives_base_name` e riorganizzate le sezioni di `gradle.properties` per maggiore chiarezza.

### Refactoring
- Rimossa la registrazione della schermata menu lato client dalla classe principale della mod (spostata in un event handler client dedicato).
- Sostituiti i nomi di classe completamente qualificati con import statici appropriati in `AdvancedEmpowererBlock`.

---

## [0.4.0] - Miglioramenti alla Logica e ai Contenuti
### Aggiunto
- **Proprietà di Orientamento Orizzontale**: Il blocco Advanced Empowerer ruota ora per essere rivolto verso il giocatore al momento del piazzamento.
- **Drop Inventario alla Rottura**: Tutti i contenuti dell'inventario (input, output, upgrade) vengono ora rilasciati correttamente nel mondo quando il blocco viene rotto.
- **Sopravvivenza alle Esplosioni**: Aggiunta una condizione di sopravvivenza alle esplosioni alla loot table dell'Advanced Empowerer, in modo che gli oggetti non vengano distrutti.
- **Localizzazione Italiana**: Aggiunta la traduzione italiana completa per tutti gli oggetti, blocchi ed etichette GUI della mod.
- **Ricetta Crafting Speed Upgrade**: Aggiunta la ricetta di crafting in-game per ottenere l'oggetto Speed Upgrade.
- **Ricetta Crafting Efficiency Upgrade**: Aggiunta la ricetta di crafting in-game per ottenere l'oggetto Efficiency Upgrade.

### Modificato
- **Miglioramento Energy Storage**: Migliorato il comportamento di `MutableEnergyStorage` e la sua sincronizzazione con il client nella block entity.
- **Layout Inventario Affinato**: Aggiustato il numero di slot e i ruoli degli slot nella block entity per maggiore chiarezza.
- **Costruttori Menu Migliorati**: Refactoring dei costruttori server e client in `AdvancedEmpowererMenu` per una migliore separazione delle responsabilità e una gestione degli slot più robusta.

---

## [0.3.1] - Documentazione e Pulizia Repository
### Aggiunto
- **Licenza MIT**: Aggiunto il file `LICENSE` al repository.
- **README**: Aggiunto il file `README.md` iniziale.
- **CHANGELOG**: Aggiunto il file `CHANGELOG` iniziale.

### Refactoring
- Riorganizzato e aggiornato il file `.gitignore` per una migliore copertura degli artefatti Java, Gradle e NeoForge.
- Riorganizzato `gradle.properties` per maggiore chiarezza e coerenza.

---

## [0.3.0] - Asset e Dati
### Aggiunto
- **Texture Blocco**: Aggiunte le texture per la faccia superiore, i lati e la faccia inferiore del blocco Advanced Empowerer.
- **Texture GUI**: Aggiunta la texture di sfondo iniziale per la schermata dell'Advanced Empowerer.
- **Texture Oggetti**: Aggiunte le texture per gli oggetti Speed Upgrade e Efficiency Upgrade.
- **Modello Blocco**: Aggiunta la definizione iniziale del modello 3D del blocco con culling delle facce corretto.
- **Definizione Blockstate**: Aggiunta la mappatura iniziale del modello nel blockstate.
- **Mappature Modello Oggetti**: Aggiunte le definizioni del modello per gli oggetti Advanced Empowerer, Speed Upgrade e Efficiency Upgrade.
- **Traduzioni Inglesi**: Aggiunte tutte le stringhe di traduzione in inglese (`en_us.json`).
- **Icona della Mod**: Aggiunta l'immagine dell'icona della mod.
- **Loot Table**: Aggiunta la loot table del blocco per l'Advanced Empowerer.
- **Ricetta Crafting (Dati)**: Aggiunta la ricetta di crafting iniziale basata su dati per l'Advanced Empowerer.
- **Metadati Resource Pack**: Aggiunto `pack.mcmeta` per il resource pack.

---

## [0.2.0] - Implementazione Core
### Aggiunto
- **`AdvancedEmpowererBlock`**: Classe blocco con interazione base al clic destro per aprire la GUI e un ticker per la block entity.
- **`AdvancedEmpowererBlockEntity`**: Implementazione completa della block entity, inclusa la gestione dell'inventario, l'accumulo di energia, il matching delle ricette (tramite `EmpowererRecipe`) e la logica di elaborazione delle ricette.
- **`AdvancedEmpowererMenu`**: Implementazione del menu container con costruttori lato server e lato client, layout degli slot e logica `quickMoveStack` (Shift+Click).
- **`AdvancedEmpowererScreen`**: Implementazione della schermata GUI con il rendering della texture di sfondo, della freccia di progresso, della barra dell'energia e del tooltip sull'energia.
- **Plugin JEI**: Aggiunto `AdvancedMachineryJEIPlugin` per l'integrazione JEI iniziale.
- **Registrazione**: Registrati la block entity, il tipo di menu, gli oggetti Speed Upgrade e Efficiency Upgrade, i block item e la scheda della modalità creativa.
- **Classe Principale**: Registrata la schermata GUI dell'Advanced Empowerer dalla classe principale della mod.

---

## [0.1.0] - Configurazione del Progetto
### Aggiunto
- **Sistema di Build Gradle**: Configurato il wrapper Gradle, il nome del progetto, il plugin NeoForge ModDev e tutte le dipendenze necessarie (NeoForge, Actually Additions, JEI).
- **`.gitignore`**: Aggiunto il file `.gitignore` configurato per progetti Java, Gradle e NeoForge.
- **`.gitattributes`**: Aggiunto il file `.gitattributes` per la gestione coerente dei fine riga tra diverse piattaforme.
- **`mods.toml`**: Aggiunta la configurazione dei metadati della mod per NeoForge (`neoforge.mods.toml`).
- **`gradle.properties`**: Aggiunte e organizzate tutte le proprietà del progetto (ID mod, versione, versione Minecraft, versioni dipendenze).
- **Commit Iniziale**: Creata la struttura base del progetto.
# Durability Tweaks

Mod kliencki na **Fabric**, który działa jak *dura pack*: zbroja zmienia kolor w miarę utraty wytrzymałości.
Pełna (nieuszkodzona) zbroja wygląda zupełnie zwyczajnie.

Autor: **betongoat** · Licencja: MIT

## Wersje

Jeden kod źródłowy, osobny plik `.jar` dla każdej grupy wersji Minecrafta:

| Jar        | Minecraft            | Java |
|------------|----------------------|------|
| `+1.21.1`  | 1.21 – 1.21.1        | 21   |
| `+1.21.3`  | 1.21.2 – 1.21.3      | 21   |
| `+1.21.4`  | 1.21.4               | 21   |
| `+1.21.5`  | 1.21.5               | 21   |
| `+1.21.8`  | 1.21.6 – 1.21.8      | 21   |
| `+1.21.10` | 1.21.9 – 1.21.10     | 21   |
| `+1.21.11` | 1.21.11              | 21   |
| `+26.1.2`  | 26.1 – 26.1.2        | 25   |
| `+26.2`    | 26.2                 | 25   |
| `+26.3`    | 26.3                 | 25   |

Wymagane: Fabric Loader i Fabric API. Opcjonalnie: [Mod Menu](https://modrinth.com/mod/modmenu) (przycisk konfiguracji na liście modów).

## Jak zmienia się kolor

**Netheryt** – od 100% do 20% kolor zmienia się co 10% uszkodzenia, od 20% co 5%.

**Diament** (i domyślnie każda inna zbroja) – co 10% od 100% do 10%, plus 2 dodatkowe kolory na intensywne PvP:
od 10% kolor zmienia się co 5%.

| Wytrzymałość | Netheryt | Diament |
|--------------|----------|---------|
| 100%         | zwykła zbroja | zwykła zbroja |
| 90–99%       | zielony | zielony |
| 80–89%       | jasnozielony | jasnozielony |
| 70–79%       | limonkowy | limonkowy |
| 60–69%       | żółtozielony | żółtozielony |
| 50–59%       | żółty | żółty |
| 40–49%       | bursztynowy | bursztynowy |
| 30–39%       | pomarańczowy | pomarańczowy |
| 20–29%       | czerwono-pomarańczowy | czerwono-pomarańczowy |
| 15–19%       | czerwony | czerwony (10–19%) |
| 10–14%       | magenta | ↑ |
| 5–9%         | fioletowy | fioletowy |
| 0–4%         | niebieski | niebieski |

## Ustawienia (GUI)

Otwierane klawiszem **K** (do zmiany w Sterowaniu) albo przyciskiem w Mod Menu.
Po lewej opcje, po prawej podgląd na żywo zbroi netherytowej i diamentowej z suwakiem wytrzymałości
oraz skala kolorów (najedź na kolor, żeby zobaczyć zakres procentów).

- **Styl** – *Pełny kolor* (cała zbroja, jak klasyczny dura pack), *Obramówka* (zbroja wygląda normalnie,
  kolorowe są tylko krawędzie), *Bez koloru*.
- **Kolory** – klasyczne, zielony → czerwony, dla daltonistów.
- **Grubość obramówki** – cienka / gruba.
- **Liczba** – wytrzymałość wypisana na hełmie albo na hełmie i napierśniku.
- **Liczba pokazuje** – procenty albo punkty wytrzymałości.
- **Liczba przy 100%** – czy pokazywać liczbę na nieuszkodzonej zbroi.
- **Zbroje** – wszystkie albo tylko diament i netheryt.

Ustawienia zapisują się w `config/durabilitytweaks.json`.

## Jak to działa

Mod nie zawiera żadnych tekstur. Kolorowe wersje są generowane w locie z tekstur aktywnego resource packa
(więc działa też z customowymi packami i zbrojami z innych modów) i podmieniane podczas renderowania zbroi –
na graczach, mobach i stojakach na zbroję. Nieużywane tekstury są zwalniane automatycznie,
a po przeładowaniu resource packów (F3+T) wszystko generuje się od nowa.

## Budowanie

Projekt używa [Stonecutter](https://stonecutter.kikugie.dev/) do budowania wielu wersji z jednego kodu.

```sh
./gradlew buildAndCollect          # wszystkie wersje -> build/libs/<wersja moda>/
./gradlew :1.21.11:buildAndCollect # jedna wersja
```

Potrzebne JDK 21 i JDK 25. GitHub Actions buduje każdą wersję i udostępnia gotowe jary jako artefakty.

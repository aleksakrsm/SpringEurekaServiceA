Problem delimičnog neuspeha izvršavanja *Remote Procedure*.
Delimični neuspeh ovde znači da funkcionalnost nekog servisa u sistemu zavisi od zahteva ka drugom servisu, koji opet za
tu funkcionalnost zavisi od trećeg servisa itd.

Cilj svega ovoga nije da se nađe neko magično rešenje koje će da izbriše grešku koja će se desiti kad jedan servis nije
dostupan - tome služe drugi mehanizmi kao što je skaliranje vertikalno i/ili horizontalno. Cilj Circuit Breaker paterna
je da spreči nepotrebno preopterećivanje servisa koji se oslanjaju servise koji nisu dostupni ili su već sami po sebi
preopterećeni.

Najprostija primena CB alata jeste da bez konfiguracije uokviri poziv klijenta i da ga izvrši, (suštinski isto kao i da
smo pozivali na standardni način) ili da vrati *fallback* vrednsot u slučaju greške.

Korisni linkovi

1. [Baeldung tutorijal](https://www.baeldung.com/spring-cloud-circuit-breaker)
2. [Netflix Hystrix](https://github.com/Netflix/Hystrix) je JVM implementacija za Circuit Breaker, vrv najpopularnija
3. [Objašnjenje da li izabrati Resilience4J ili Hystrix](https://stackoverflow.com/questions/70587963/resilience4j-vs-hystrix-what-would-be-the-best-for-fault-tolerance) -
   TL;DR: Hystrix se ne razvija više aktivno već je u Maintenance mode od 2018 dakle možda nije najbolja opcija jer se
   priprema njegovo napuštanje

## Funkcionisanje Circuit Breaker-a

Circuit breaker može da bude u 3 stanja:

1. Closed State: U ovom stanju Circuit Breaker ne preduyima ništa sem što prati broj zahteva koji nije poršao kako bi
   znao da li treba da pormeni stanje i preduzme dalje akcije.
2. Open State: U ovo stanje CB ulazi onda kada broj neuspešnih zahteva premaši neki treshold, npr 50%. U ovom trenutku
   su pozivi ka tom servisu blokirani kako bi se izbeglo preopterećivanje servisa.
3. Half Open State: Ovo je stanje oporavka nakon Open State kada Circuit breaker propušta neki određeni broj zahteva
   kako bi uočio da li se servis oporavio. Ovo stanje se aktivira nakon što je isteklo vreme definisano sa
   `waitDurationInOpenState`.
    - Ovde može da se definiše `permittedNumberOfCallsInHalfOpenState` koji određuje koliko zahteva puštamo da bismo
      testirali da li se servis oporavio
    -

## Implementacija:

Da bi se Circuit Breaker pattern koristio treba uvesti njegovu implementaciju.
Postoji više varijanti implementacija:

1. [Hystrix](https://www.baeldung.com/introduction-to-hystrix) (biće deprecated uskoro)
2. [Resilience4J](https://www.baeldung.com/resilience4j)
3. [Spring Retry](https://www.baeldung.com/spring-retry)
4. [Sentinel](https://www.baeldung.com/java-sentinel-intro)
   Od ove 4 varijante Spring Cloud Circuit Breaker apstrakcija ima podršku za **2.** i **3.**
   Zato sam ja u demonstraciji koristio *Resilience4J* jer se čii kao najpopularniji trenutno.

### Demo - basic

Koristim RestClient implementaciju i zato koristim skledeću biblioteku:

```
<dependency>  
    <groupId>org.springframework.cloud</groupId>  
    <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>  
</dependency>
```

Kada je ova biblioteka na classpath-u implementacija interfejsa `CircuitBreakerFactory` koji ću koristiti da napravim
`CircuitBreaker` biće `Resilience4JCircuitBreakerFactory`.

```
circuitBreaker = circuitBreakerFactory.create(serviceId);
```

Pravim CB sa nekim identifikatorom koji treba da bude što bolje definisan za razliku od ovog primera.
Ukoliko je uspešno definisan CB možemo uokviriti poziv ka ekternom api-ju koristeći sledeću notaciju:

```
List<AlbumsRestClient.Album> albums = circuitBreaker.run(albumsRestClient::getAlbums,  
        throwable -> Collections.emptyList());  
log.info("Retrieved {} items from external API with circuit breaker", albums.size());  
return ResponseEntity.ok(new AlbumsResponse(serviceId + ":" + servicePort, albums));
```

### Dodatna konfiguracija

Dosadašnja implementacija ne nudi nikakvu specijalnu implementaciju, međutim svaka od implementacija nudi dodatne opcije
za konfigurisanje.

[Referentna dokumentacija](https://docs.spring.io/spring-cloud-circuitbreaker/reference/spring-cloud-circuitbreaker-resilience4j.html)
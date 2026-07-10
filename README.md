# CopyItem

1.21 (Fabric) için öğretici/bilgilendirici amaçlı basit bir mod.

## Komutlar
- `/copyitem` → elinizdeki itemin adını ve kullanım bilgisini gösterir.
- `/copyitem <miktar>` → elinizdeki itemi **silmeden**, belirtilen miktarda kopyalayıp envanterinize ekler.

Varsayılan olarak komut permission level **2** (op) gerektirir; `CopyItemMod.java` içindeki `hasPermissionLevel(2)` satırından değiştirebilirsin.

## Derleme
Wrapper jar bu ortamda internet erişimi olmadığı için eklenemedi. İki seçenek var:

**A) GitHub Actions (senin normal akışın):**
Repoyu push et, `.github/workflows/build.yml` otomatik derleyip jar'ı Actions → Artifacts kısmına koyar. Gradle wrapper'a ihtiyaç yok, Gradle CI üzerinde kurulu geliyor.

**B) Kendi makinende/Termux'ta:**
```
gradle wrapper --gradle-version 8.10
./gradlew build
```
Jar `build/libs/copyitem-1.0.0.jar` altında oluşur.

## Notlar
- `minecraft_version`, `yarn_mappings`, `loader_version`, `fabric_version` değerlerini `gradle.properties` içinden, gerekirse fabricmc.net/develop üzerinden güncel değerlerle değiştir.
- 1.21.x üstü diğer sürümler için sadece bu properties dosyasındaki versiyonları güncellemen yeterli, kod tarafında değişiklik gerekmiyor.

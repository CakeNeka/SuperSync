---
marp: true
theme: gaia
class: 
    - lead
--- 

<style>
p, h1, h2, h3, li{
    font-size: 0.94em;
}
</style>

## <!-- fit --> $SuperSync$



---

<!-- 
    class: invert
-->



![bg contain](./media/super_sync.gif)

---

    
## 1. 📐 Diseño

- **Enlace:** [Github](https://github.com/cakeneka/SuperSync)
- **Lenguaje de programación:** Java
- **Tamaño:** 1 clase, 263 líneas de código
- **Librerías:** Apache Commons Net

La aplicación **sincroniza** una carpeta local con una carpeta en un servidor FTP.

La carpeta en remoto refleja **todos** los cambios de la carpeta local excepto por los **directorios vacios**

---

## 2. ⚙ Análisis Funcionamiento

El sistema se ejecuta cada cierta cantidad de segundos (en el ejemplo anterior, 4 segundos).
Comprueba que ficheros locales han sido modificados con respecto a los ficheros remotos:

- Si el archivo local no existe en el servidor, sube el archivo local.
- Si el archivo local tiene una fecha de modificación más reciente que el archivo del servidor, sube el archivo local.
- Si tanto el archivo local como el remoto tienen la misma fecha de modificación, no se realizan cambios.

---

### Subir un archivo

Cuando subes un archivo al servidor FTP, el servidor FTP sobreescribe la fecha de última modificación del archivo. 
El siguiente código crea los directorios necesarios, sube el archivo, y por último reasigna la fecha modificación del fichero remoto.

---

```java
private void upload(File localFile) throws IOException {
    Logger.logMessage("Uploading " + localFile);
    String ftpPath = toFtpPath(localFile);

    // Crear directorios padre en el servidor si es necesario
    String ftpPathParents = ftpPath.substring(0, ftpPath.lastIndexOf('/'));
    ftpCreateDirectoryTree(ftpPathParents);
    // Subir archivo
    ftpClient.changeWorkingDirectory("/");
    InputStream is = new FileInputStream(localFile);
    ftpClient.storeFile(ftpPath, is);
    is.close();

    // Establecer la misma fecha de modificación que en el archivo local
    String ftpDate = timeStampToString(localFile.lastModified());
    ftpClient.setModificationTime(ftpPath, ftpDate);
}
```

---


---

## 📚 Recursos

No he utilizado IAs. A continuación dejo algunos enlaces que me han sido de utilidad.
package util;

import io.reactivex.schedulers.Schedulers;
import model.Photo;
import org.apache.tika.Tika;

import io.reactivex.*;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class PhotoDownloader {

    private static final Logger log = Logger.getLogger(PhotoDownloader.class.getName());

    public Observable<Photo> getPhotoExamples() throws IOException {
        return Observable.just("https://i.ytimg.com/vi/7uxQjydfBOU/hqdefault.jpg",
                "http://digitalspyuk.cdnds.net/16/51/1280x640/landscape-1482419524-12382542-low-res-sherlock.jpg",
                "http://image.pbs.org/video-assets/pbs/masterpiece/132733/images/mezzanine_172.jpg",
                "https://classicmystery.files.wordpress.com/2016/04/miss-marple-2.jpg",
                "https://i.pinimg.com/736x/7c/14/c9/7c14c97839940a09f987fbadbd47eb89--detective-monk-adrian-monk.jpg")
                .map(photoUrl -> getPhoto(photoUrl));
    }

    public Observable<Photo> searchForPhotos(String searchQuery) throws IOException {
        return Observable.create(observer -> {
            try{
                String googleSiteSource = downloadUrlSource(searchQuery);
                List<String> photoUrls = extractPhotoUrls(googleSiteSource);
//                try (BufferedReader br = new BufferedReader(new FileReader("urls.txt"))) {
//                    String line;
//                    while ((line = br.readLine()) != null) {
//                        photoUrls.add(line);
//                    }
//                }
                for (String photoUrl : photoUrls) {
                    try {
                        observer.onNext(getPhoto(photoUrl));
                    } catch (IOException e) {
                        log.log(Level.WARNING, "Could not download a photo", e);
                    }
                }
                observer.onComplete();
            } catch (Exception e){
                observer.onError(e);
            }
        });
    }

    public Observable<Photo> searchForPhotos(List<String> searchQueries) throws IOException {
        Observable<Photo> photos = Observable.empty();

        for (String searchQuery : searchQueries){
            photos = Observable.merge(photos, searchForPhotos(searchQuery).subscribeOn(Schedulers.io()));
        }
        return photos;
    }

    private Photo getPhoto(String photoUrl) throws IOException {
        log.info("Downloading... " + photoUrl);
        byte[] photoData = downloadPhoto(photoUrl);
        return createPhoto(photoData);
    }

    private Photo createPhoto(byte[] photoData) throws IOException {
        Tika tika = new Tika();
        String fileType = tika.detect(photoData);
        if (fileType.startsWith("image")) {
            return new Photo(LocalDate.now(), fileType.substring(fileType.indexOf("/") + 1), photoData);
        }
        throw new IOException("Unsupported media type: " + fileType);
    }

    private String downloadUrlSource(String searchQuery) throws IOException {
        URL url = new URL(String.format("https://www.google.com/search?q=%s&tbm=isch", searchQuery.replace(' ', '%')));
        URLConnection connection = url.openConnection();
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        StringBuilder sb = new StringBuilder();
        try (BufferedReader in = new BufferedReader(new InputStreamReader(
                connection.getInputStream(), "UTF-8"))) {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                sb.append(inputLine);
            }
        }
        return sb.toString();
    }

    private List<String> extractPhotoUrls(String html) {
        List<String> urls = new ArrayList<>();

        int imageUrlStartIndex = html.indexOf("\"ou\"");

        while (imageUrlStartIndex >= 0) {
            imageUrlStartIndex = html.indexOf("\"", imageUrlStartIndex + 4);
            imageUrlStartIndex++;
            int imageUrlEndIndex = html.indexOf("\"", imageUrlStartIndex);
            String url = html.substring(imageUrlStartIndex, imageUrlEndIndex);
            urls.add(url);
            imageUrlStartIndex = html.indexOf("\"ou\"", imageUrlEndIndex);
        }
        return urls;
    }

    private byte[] downloadPhoto(String url) throws IOException {
        URL photoUrl = new URL(url);
        URLConnection yc = photoUrl.openConnection();
        yc.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.11 (KHTML, like Gecko) Chrome/23.0.1271.95 Safari/537.11");
        try (InputStream inputStream = yc.getInputStream()) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];

            while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            return buffer.toByteArray();
        }
    }
}
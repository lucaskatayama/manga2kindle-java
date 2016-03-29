package br.com.lucaskatayama;

import org.apache.commons.io.FileUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;

public class Driver {

	static void updateProgress(double progressPercentage) {
		final int width = 50; // progress bar width in chars

		System.err.print("\r[");
		int i = 0;
		for (; i <= (int)(progressPercentage*width); i++) {
			System.err.print("=");
		}
		System.err.print(">");
		for (; i < width; i++) {
			System.err.print(" ");
		}
		System.err.print("]");
		System.err.printf("%.0f%%", progressPercentage*100);
	}

	public static void main(String[] args) {
		String downloadDir = "./";
		String chapter = "667";
		String manga = "one-piece";
		String tmpDir = downloadDir+"/.tmp";

		HttpClient client = new DefaultHttpClient();
		HttpGet get = new HttpGet("http://www.mangahit.com/manga/"+manga);

		try {
			HttpResponse response = client.execute(get);
			Document doc = Jsoup.parse(EntityUtils.toString(response.getEntity()));

			Elements els = doc.select("tr.line.record");

			Iterator<Element> itr = els.iterator();
			Element elA;
			String link = "";
			while(itr.hasNext()){
				Element el = itr.next();
				elA = el.select("a").first();
				if(elA.text().contains(chapter)){
					link = elA.attr("abs:href");
					break;
				}
			}

			if(link == null){
				return;
			}

			/*Obtem a lista de imagens*/

			get = new HttpGet(link);
			response = client.execute(get);
			doc = Jsoup.parse(EntityUtils.toString(response.getEntity()));

			els = doc.select("img[src*=img.mangahit]");
			Element pages;
			int pagesInt = 0;
			if(els.size() > 0){
				link = els.first().attr("src");
				link = link.substring(0, link.lastIndexOf("/"));
				pages = doc.select("select#pages").select("option").last();
				pagesInt = Integer.parseInt(pages.attr("value"));

			}


			/*Remove pasta temporaria*/
			FileUtils.deleteDirectory(new File(tmpDir));
			FileUtils.forceMkdir(new File(tmpDir));
			int fileNum = 1;
			String file = String.format("%02d.jpg", fileNum);
			System.err.println("Downloading...");
			updateProgress((double)fileNum/pagesInt);
			while(true){
				try {
					FileUtils.copyURLToFile(new URL(String.format("%s/%s", link, file)),new File(String.format("%s/%s", tmpDir, file)));
				}catch (IOException e){
					break;
				}
				updateProgress((double)fileNum/pagesInt);
				fileNum++;
				file = String.format("%02d.jpg", fileNum);
			}
			System.err.printf("\nFinished. ");
			/*convert as imagens*/
			Process pr = Runtime.getRuntime().exec(String.format("convert %s/*.jpg %s/%s_%s.pdf", tmpDir, downloadDir, manga, chapter));
			pr.waitFor();

			/*Deleta os arquivos de imagens*/
			FileUtils.deleteDirectory(new File(tmpDir));


		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}


	}
}

package player;

import java.net.*;
import java.util.*;
import java.io.*;


/**
 * Classe com metodos auxiliares para processar pedidos HTTP
 */
public class HTTPUtilities {


	public static String readLine( InputStream is ) throws IOException {
		StringBuffer sb = new StringBuffer() ;
		
		int c ;
		while( (c = is.read() ) >= 0 ) {
			if( c == '\r' ) continue ;
			if( c == '\n' ) break ;
			sb.append( new Character( (char)c) ) ;
		}
		return sb.toString() ;
	} 
	/**
	 * A partir da linha de pedido HTTP devolve um array com o metodo invocado
	 * na posicao 0, o URL na posicao 1, e a versao na posicao 2 Por ex., para
	 * "GET http://www.unl.pt/index.html HTTP/1.0", devolve [0]->"GET";
	 * [1]->"http://www.unl.pt/index.html"; [2]->"HTTP/1.0" Caso request nao
	 * seja um pedido HTTP valido, devolve [0] -> { "ERROR", "", "" }
	 * NOTA: Caso a versao HTTP nao seja especificada, [2] = ""
	 */
	public static String[] parseHttpRequest( String request) {
		String[] result = { "ERROR", "", "" };
		int pos0 = request.indexOf( ' ');
		if( pos0 == -1)
			return result;
		result[0] = request.substring( 0, pos0).trim();
		pos0++;
		int pos1 = request.indexOf( ' ', pos0);
		if( pos1 == -1) {
			result[1] = request.substring( pos0).trim();
			result[2] = null;
		} else {
			result[1] = request.substring( pos0, pos1).trim();
			result[2] = request.substring( pos1 + 1).trim();
		}
		return result;
	}

	/**
	 * A partir um string com um URL devolve um array com as componentes
	 * host e port do URL. 
	 * Por exemplo, se receber "http://www.unl.pt:800/index.html
	 * devolve: [0] = "www.unl.pt" [1] = "800"
	 * Se a porta estiver omitida, devolve o valor por defeito
	 */
	public static String[] parseURL ( String url ) {
		String[] result = { "unknown", "80" };
		int pos0 = url.indexOf( '/');
		if( pos0 == -1) return result;
		pos0 += 2;
		int posPort = url.indexOf( ':', pos0);
		if( posPort == -1) {
			// there is no specific port
			int posSlash = url.indexOf( '/', pos0);
			if( posSlash == -1) {
				result[0] = url.substring(pos0);
			} else {
				result[0] = url.substring(pos0, posSlash );
			}
		} else {
			// there is a specific port
			result[0] = url.substring( pos0, posPort );
			int posSlash = url.indexOf( '/', pos0);
			if( posSlash == -1) {
				result[1] = url.substring(posPort) ;
			} else {
				result[1] = url.substring( posPort + 1,posSlash );
			}
		}
		return result;
	}

	
	/**
	 * A partir de um header HTTP, devolve um array com o nome do header na
	 * posicao 0 e o valor indicado na posicao 1 Por ex., para "Connection:
	 * Keep-alive", devolve [0]->"Connection"; [1]->"Keep-alive" Caso header nao
	 * n�o pare�a um header HTTP v�lido com ":" no meio devolve { "ERROR", "" }.
	 */
	public static String[] parseHttpHeader( String header) {
		String[] result = { "ERROR", "" };
		int pos0 = header.indexOf( ':');
		if( pos0 == -1)
			return result;
		result[0] = header.substring( 0, pos0).trim();
		result[1] = header.substring( pos0 + 1).trim();
		return result;
	}

	/**
	 * A partir de uma string com o conteudo de um form submetido no formato
	 * application/x-www-form-urlencoded, devolve um objecto do tipo Properties,
	 * associando a cada elemento do form o seu valor
	 */
	public static Properties parseHttpPostContents( String contents)
			throws IOException {
		Properties props = new Properties();
		Scanner scanner = new Scanner( contents).useDelimiter( "&");
		while( scanner.hasNext()) {
			Scanner inScanner = new Scanner( scanner.next()).useDelimiter( "=");
			String propName = URLDecoder.decode( inScanner.next(), "UTF-8");
			String propValue = "";
			try {
				propValue = URLDecoder.decode( inScanner.next(), "UTF-8");
			} catch( Exception e) {
				// do nothing
			}
			props.setProperty( propName, propValue);
		}
		return props;
	}

}

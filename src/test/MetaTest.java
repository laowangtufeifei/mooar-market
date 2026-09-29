import com.mooar.market.service.NftMetadataService;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

public class MetaTest {

    public static void main(String[] args) {
        Web3j build = Web3j.build(new HttpService("https://lb5.stepn.com"));
        NftMetadataService nftMetadataService = new NftMetadataService(build);
        String s = nftMetadataService.fetchMetadata("0x483EA031160638Ae54a036ac589D9Eb31c121A8C", 77649281);
        String s2 = nftMetadataService.fetchMetadata("0x462DD5C6A786aCB47c60ad419FeaB11054B0b249", 85520);
        String s3 = nftMetadataService.fetchMetadata("0xB48f53010Acbc0E24D3D12D892E2215879e6fD13", 40450);
        System.out.println(s);
        System.out.println(s2);
        System.out.println(s3);
    }
}

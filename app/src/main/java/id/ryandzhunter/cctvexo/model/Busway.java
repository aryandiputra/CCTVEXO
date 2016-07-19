
package id.ryandzhunter.cctvexo.model;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Busway {

    @SerializedName("id")
    @Expose
    private int id;
    @SerializedName("koridor")
    @Expose
    private String koridor;
    @SerializedName("url")
    @Expose
    private String url;
    @SerializedName("cctv_url")
    @Expose
    private String cctvUrl;
    @SerializedName("lat")
    @Expose
    private String lat;
    @SerializedName("lng")
    @Expose
    private String lng;
    @SerializedName("ip_public")
    @Expose
    private String ipPublic;
    @SerializedName("kode_area")
    @Expose
    private String kodeArea;
    @SerializedName("area")
    @Expose
    private String area;
    @SerializedName("sub_area")
    @Expose
    private String subArea;
    @SerializedName("ket")
    @Expose
    private String ket;

    /**
     * 
     * @return
     *     The id
     */
    public int getId() {
        return id;
    }

    /**
     * 
     * @param id
     *     The id
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * 
     * @return
     *     The koridor
     */
    public String getKoridor() {
        return koridor;
    }

    /**
     * 
     * @param koridor
     *     The koridor
     */
    public void setKoridor(String koridor) {
        this.koridor = koridor;
    }

    /**
     * 
     * @return
     *     The url
     */
    public String getUrl() {
        return url;
    }

    /**
     * 
     * @param url
     *     The url
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * 
     * @return
     *     The cctvUrl
     */
    public String getCctvUrl() {
        return cctvUrl;
    }

    /**
     * 
     * @param cctvUrl
     *     The cctv_url
     */
    public void setCctvUrl(String cctvUrl) {
        this.cctvUrl = cctvUrl;
    }

    /**
     * 
     * @return
     *     The lat
     */
    public String getLat() {
        return lat;
    }

    /**
     * 
     * @param lat
     *     The lat
     */
    public void setLat(String lat) {
        this.lat = lat;
    }

    /**
     * 
     * @return
     *     The lng
     */
    public String getLng() {
        return lng;
    }

    /**
     * 
     * @param lng
     *     The lng
     */
    public void setLng(String lng) {
        this.lng = lng;
    }

    /**
     * 
     * @return
     *     The ipPublic
     */
    public String getIpPublic() {
        return ipPublic;
    }

    /**
     * 
     * @param ipPublic
     *     The ip_public
     */
    public void setIpPublic(String ipPublic) {
        this.ipPublic = ipPublic;
    }

    /**
     * 
     * @return
     *     The kodeArea
     */
    public String getKodeArea() {
        return kodeArea;
    }

    /**
     * 
     * @param kodeArea
     *     The kode_area
     */
    public void setKodeArea(String kodeArea) {
        this.kodeArea = kodeArea;
    }

    /**
     * 
     * @return
     *     The area
     */
    public String getArea() {
        return area;
    }

    /**
     * 
     * @param area
     *     The area
     */
    public void setArea(String area) {
        this.area = area;
    }

    /**
     * 
     * @return
     *     The subArea
     */
    public String getSubArea() {
        return subArea;
    }

    /**
     * 
     * @param subArea
     *     The sub_area
     */
    public void setSubArea(String subArea) {
        this.subArea = subArea;
    }

    /**
     * 
     * @return
     *     The ket
     */
    public String getKet() {
        return ket;
    }

    /**
     * 
     * @param ket
     *     The ket
     */
    public void setKet(String ket) {
        this.ket = ket;
    }

}

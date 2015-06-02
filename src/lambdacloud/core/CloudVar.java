package lambdacloud.core;

import io.netty.channel.Channel;
import lambdacloud.net.CloudQuery;
import lambdacloud.net.CloudVarHandler;
import lambdacloud.net.CloudVarRespHandler;
import lambdacloud.net.CloudVarResp;
import lambdacloud.net.CloudClient;
import symjava.bytecode.BytecodeBatchFunc;
import symjava.symbolic.Expr;
import symjava.symbolic.Symbol;
import symjava.symbolic.utils.JIT;
import symjava.symbolic.utils.Utils;

/**
 * An instance of CloudVar represents a variable on the cloud server.
 * The variable can be created on local and stored to cloud side.
 * For example:
 * <p><blockquote><pre>
 *     CloudVar var = new CloudVar("myvar").init(new double[]{1, 2, 3, 4, 5});
 *     var.sotoreToCloud();
 * </pre></blockquote>
 * <P>A variable on the cloud can be fetched to local by providing the correct name.
 * <p><blockquote><pre>
 *     if(var.fetchToLocal()) {
 *       for(double d : var.getData()) {
 *         System.out.println(d);
 *       }
 *     }
 * </pre></blockquote>
 *
 */
public class CloudVar extends Symbol {
	double[] data = new double[0];
	boolean isOnCloud = false;
	
	public CloudVar() {
		super("CloudVar"+java.util.UUID.randomUUID().toString().replaceAll("-", ""));
	}

	public CloudVar(String name) {
		super(name);
	}
	
	public CloudVar(Expr expr) {
		super("CloudVar"+java.util.UUID.randomUUID().toString().replaceAll("-", ""));
		this.compile(this.label, expr);
	}
	
	public CloudVar(String name, Expr expr) {
		super(name);
		this.compile(name, expr);
	}
	
	public CloudVar compile(String name, Expr expr) {
		if(CloudConfig.isLocal()) {
			CloudVar[] args = Utils.extractCloudVars(expr).toArray(new CloudVar[0]);
			BytecodeBatchFunc fexpr = JIT.compileBatchFunc(args, expr);
			data = new double[args[0].size()];
			fexpr.apply(data, 0, Utils.getDataFromCloudVars(args));
		} else {
			//expr contains server references
		}
		return this;
	}

	/**
	 * Initialize the cloud variable with the given array.
	 * The new cloud variable simply wrap the array; that is,
	 * it is backed by the given array. Any modifications to the 
	 * cloud variable will cause the array to be modified and vice versa.
	 * @param array
	 * @return
	 */
	public CloudVar init(double ...array) {
		this.data = array;
		return this;
	}

	/**
	 * Set the value of the backed array at index
	 * @param index
	 * @param value
	 */
	public void set(int index, double value) {
		data[index] = value;
	}
	
	/**
	 * Get the value of the backed array at index
	 * @param index
	 * @return
	 */
	public double get(int index) {
		return data[index];
	}
	 
	/**
	 * Resize the backed array. Old data will be copied to the new backed array
	 * if the new size is larger than the old size otherwise the data that beyond
	 * the new size will be discarded.
	 * @param size
	 * @return
	 */
	public CloudVar resize(int size) {
		if(this.data == null)
			this.data = new double[size];
		else {
			double[] newdata = new double[size];
			if(size > data.length) {
				System.arraycopy(this.data, 0, newdata, 0, this.data.length);
			} else {
				System.arraycopy(this.data, 0, newdata, 0, size);
			}
			this.data = newdata;
		}
		return this;
	}
	
	/**
	 * Return the length of the backed array
	 * @return
	 */
	public int size() {
		return data.length;
	}
	
	/**
	 * Return the length of the backed array
	 * @return
	 */
	public int length() {
		return data.length;
	}
	
	/**
	 * Return the name of the cloud variable. The name is the identifier
	 * of the cloud variable on the cloud server. Any local instance of 
	 * CloudVar has the same name will be assumed to be the same vaiable 
	 * on the cloud side.
	 * @return
	 */
	public String getName() {
		return this.label;
	}
	
	/**
	 * Return the backed array
	 * @return
	 */
	public double[] getData() {
		return data;
	}
	
	/**
	 * Store the local variable to the cloud. 
	 */
	public boolean storeToCloud() {
		CloudClient client = CloudConfig.getClient();
		CloudVarRespHandler handler = client.getCloudVarRespHandler();
		try {
			client.getChannel().writeAndFlush(this).sync();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		CloudVarResp resp = handler.getCloudResp();
		if(resp.status == 0)
			this.isOnCloud = true;
		else
			this.isOnCloud = false;
		return this.isOnCloud;
	}
	
	/**
	 * Fetch a cloud variable to local. The name of the variable 
	 * on the cloud must be specified. Return true if success.
	 * Call getData() to access the data in the cloud variable
	 * @return
	 */
	public boolean fetchToLocal() {
		if(CloudConfig.isLocal())
			return true;
		else {
			CloudClient client = CloudConfig.getClient();
			Channel ch = client.getChannel();
			CloudQuery qry = new CloudQuery();
			qry.objName = this.getLabel();
			qry.qryType = CloudQuery.CLOUD_VAR;
			try {
				ch.writeAndFlush(qry).sync();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			CloudVarHandler h = client.getCloudVarHandler();
			CloudVar var = h.getCloudVar();
			this.data = var.data;
			this.isOnCloud = var.isOnCloud();
			return this.isOnCloud;
		}
	}
	
	public boolean isOnCloud() {
		return isOnCloud;
	}
	
	public void setOnCloudFlag(boolean flag) {
		this.isOnCloud = flag;
	}
	
	@Override
	public Expr simplify() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean symEquals(Expr other) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Expr diff(Expr expr) {
		// TODO Auto-generated method stub
		return null;
	}
	
	public static CloudVar valueOf(Expr expr) {
		return new CloudVar(expr);
	}

}

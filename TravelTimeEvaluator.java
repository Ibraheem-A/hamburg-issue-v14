package sim.analysis;

import org.matsim.vehicles.Vehicle;
import sim.dataPreparation.FreightController;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.events.*;
import org.matsim.api.core.v01.events.handler.*;
import org.matsim.api.core.v01.network.Link;

import java.util.Hashtable;
import java.util.LinkedList;

public class TravelTimeEvaluator implements LinkEnterEventHandler, LinkLeaveEventHandler, VehicleEntersTrafficEventHandler, VehicleLeavesTrafficEventHandler {

	private static final Logger LOG = LogManager.getLogger(TravelTimeEvaluator.class.getName());

	public static final TravelTimeEvaluator instance = new TravelTimeEvaluator();
	
	
	
	private Hashtable<Vehicle, LinkedList<TravelTimeDataVector>> allTimes = new Hashtable<>();
	
	//Using DataVectors here as well; even though I have some redundancy then I'll still save a ton of ram since I'm reusing the instances
	private Hashtable<Link, LinkedList<TravelTimeDataVector>> linkReferencedTimes = new Hashtable<>();
	
	
	private TravelTimeEvaluator() {}
	
	
	//getters
	public Hashtable<Vehicle, LinkedList<TravelTimeDataVector>> getAllTimes() {
		return allTimes;
	}
	
	public Hashtable<Link, LinkedList<TravelTimeDataVector>> getLinkReferencedTimes() {
		return linkReferencedTimes;
	}
	
	
	//setters
	@Override
	public void reset(int iteration) {
		allTimes = new Hashtable<>();
		linkReferencedTimes = new Hashtable<>();
	}
	
	
	//enter
	@Override
	public void handleEvent(VehicleEntersTrafficEvent event) {
		linkEnter(event.getVehicleId(), event.getLinkId(), event.getTime() + 1); //this should make the time for this starting link 0
	}

	@SuppressWarnings("deprecation")
	@Override
	public void handleEvent(LinkEnterEvent event) {
		linkEnter(event.getVehicleId(), event.getLinkId(), event.getTime());
	}
	
	private void linkEnter(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		//person
		Vehicle vehicle = FreightController.get().getMATSimControler().getScenario().getVehicles().getVehicles().get(vehicleId);
		if(vehicle == null) {
			LOG.warn("Received vehicle id \"" + vehicleId + "\" from event that was not mapped to a person in the population; ignoring this event");
			return;
		}
		
		//data
		Link link = FreightController.get().getNetwork().getLinks().get(linkId);
		if(link == null) {
			LOG.warn("Received link id \"" + linkId + "\" from event that was not mapped to a link in the network; ignoring this event");
			return;
		}
		
		TravelTimeDataVector v = new TravelTimeDataVector();
		v.link = link;
		v.enterTime = time;
		
		//adding to tables
		LinkedList<TravelTimeDataVector> ll = linkReferencedTimes.get(link);
		if(ll == null) {
			//this only happens the first time we encounter this link
			ll = new LinkedList<>();
			linkReferencedTimes.put(link, ll);
		}
		ll.add(v);
		
		LinkedList<TravelTimeDataVector> pl = allTimes.get(vehicle);
		if(pl == null) {
			//this only happens the first time we encounter this person
			pl = new LinkedList<>();
			allTimes.put(vehicle, pl);
		}
		pl.add(v);
	}
	

	//leave
	@Override
	public void handleEvent(VehicleLeavesTrafficEvent event) {
		linkLeave(event.getVehicleId(), event.getLinkId(), event.getTime());
	}

	@SuppressWarnings("deprecation")
	@Override
	public void handleEvent(LinkLeaveEvent event) {
		linkLeave(event.getVehicleId(), event.getLinkId(), event.getTime());
	}
	
	private void linkLeave(Id<Vehicle> vehicleId, Id<Link> linkId, double time) {
		//person
		Vehicle vehicle = FreightController.get().getMATSimControler().getScenario().getVehicles().getVehicles().get(vehicleId);
		if(vehicle == null) {
			LOG.warn("Received person id \"" + vehicleId + "\" from event that was not mapped to a person in the population; ignoring this event");
			return;
		}
		
		Link link = FreightController.get().getNetwork().getLinks().get(linkId);
		if(link == null) {
			LOG.warn("Received link id \"" + linkId + "\" from event that was not mapped to a link in the network; ignoring this event");
			return;
		}
		
		LinkedList<TravelTimeDataVector> l = allTimes.get(vehicle);
		if(l == null) {
			LOG.warn("Person from event has not yet been mapped to a data vector list; ignoring this event");
			return;
		}
		if(l.size() == 0) {
			LOG.warn("List person from event is mapped to has no elements; ignoring this event");
			return;
		}
		
		TravelTimeDataVector v = l.get(l.size() - 1);
		
		if(v.link != link) {
			LOG.warn("Last element of vector list mapped to person from event refers to the wrong link; ignoring this event");
			return;
		}
		
		v.leaveTime = time;
	}
	
	
	//classes
	public static class TravelTimeDataVector {
		
		private Link link;
		private double enterTime = -1;
		private double leaveTime = -1;
		
		//getters
		public Link getLink() {
			return link;
		}
		
		public double getEnterTime() {
			return enterTime;
		}
		
		public double getLeaveTime() {
			return leaveTime;
		}
		
		@Override
		public String toString() {
			return "(" + link.getId() + " |" + enterTime + "|" + leaveTime + "|" + (leaveTime - enterTime) + ")";
		}
	}
}

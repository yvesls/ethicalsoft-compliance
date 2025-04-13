import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';

@Component({
  selector: 'app-terms',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './terms.component.html',
  styleUrl: './terms.component.scss'
})
export class TermsComponent implements OnInit {

  termsContent = `Lorem ipsum dolor sit amet, consectetur adipiscing elit. Aliquam id turpis nunc. Suspendisse ultricies felis at interdum convallis. In ac ex nec felis mollis sollicitudin. Quisque ac lectus at nibh porta dignissim. Curabitur mauris quam, pharetra sit amet fringilla vel, posuere sit amet turpis. Fusce quam ipsum, vulputate in lorem vel, aliquam consequat nisi. Duis nec nulla placerat eros pretium viverra ac a lectus. Vivamus quis elementum augue. Nulla a sagittis tellus. In ut pretium arcu. Suspendisse potenti. Curabitur orci massa, feugiat non nibh in, consequat rutrum nisi. Etiam rhoncus, libero vel dignissim congue, ante nunc semper enim, in rutrum lacus lectus hendrerit nunc. Suspendisse eleifend dapibus eros sit amet sollicitudin. Fusce dictum quam eu elit pellentesque eleifend. Vivamus pellentesque, risus quis convallis placerat, enim eros egestas magna, at sodales diam ipsum et quam.
Cras vehicula purus urna, non elementum arcu tincidunt et. Pellentesque suscipit mi condimentum erat accumsan, sed posuere odio luctus. Mauris quis dapibus felis, ut blandit tortor. Vivamus ac erat dapibus, ultrices libero non, consequat est. Sed in lobortis urna, eu rutrum sem. Duis elit eros, pulvinar quis sagittis non, hendrerit sit amet nunc. Quisque id tortor eu risus pellentesque lobortis. Vivamus in lorem turpis. In aliquam ligula ut elementum pharetra. Curabitur eu ornare massa. Nunc malesuada suscipit nibh sit amet mollis. Nullam ut mauris lorem. Nam in ex et tortor semper lacinia. Etiam pulvinar urna sit amet nisi fringilla convallis. Aenean laoreet mollis tristique. Nullam vitae hendrerit diam.
Cras tincidunt aliquam purus, in ornare leo varius in. Aliquam vitae feugiat elit, ut dignissim massa. Maecenas tempus scelerisque sem ac iaculis. Etiam pulvinar lectus in dictum tempus. Aenean maximus, urna non lobortis eleifend, odio odio congue nisi, sit amet ornare elit orci id enim. Phasellus quis ipsum tortor. Sed euismod velit quam, vitae pellentesque enim pulvinar quis. In tortor neque, euismod vel rhoncus nec, laoreet eu nunc.
Quisque at mauris turpis. Proin suscipit lacus non augue convallis accumsan. Donec placerat mauris nec sem convallis convallis. Pellentesque accumsan diam at orci accumsan feugiat. Cras eu scelerisque nisi. Quisque ornare eget tortor eget ornare. Curabitur finibus lacus at bibendum aliquet. Nam eget molestie neque. Aenean eget congue ex. Sed placerat consectetur neque, sed hendrerit ante pharetra vitae. Pellentesque efficitur sem eu est ultrices, vel facilisis odio dictum. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Integer interdum eros vitae pulvinar iaculis.
Cras tincidunt aliquam purus, in ornare leo varius in. Aliquam vitae feugiat elit, ut dignissim massa. Maecenas tempus scelerisque sem ac iaculis. Etiam pulvinar lectus in dictum tempus. Aenean maximus, urna non lobortis eleifend, odio odio congue nisi, sit amet ornare elit orci id enim. Phasellus quis ipsum tortor. Sed euismod velit quam, vitae pellentesque enim pulvinar quis. In tortor neque, euismod vel rhoncus nec, laoreet eu nunc.
Orci varius natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Maecenas sem mi, sodales tristique placerat a, vestibulum vel massa. Nunc imperdiet orci lorem, eget finibus risus sodales nec. Vestibulum lacinia dolor ex, vel lobortis arcu aliquam in. Etiam volutpat sapien consequat ante consequat, ut pulvinar sapien faucibus. Ut sed sem orci. Morbi eget dui id diam cursus feugiat et eget nisl. Etiam fermentum nunc eu mi ornare imperdiet. Morbi tincidunt nibh leo, sodales egestas ligula tempor vitae.
Suspendisse potenti. Aliquam tincidunt magna magna, suscipit sagittis augue posuere sit amet. Aenean lobortis luctus sodales. Praesent efficitur tortor ligula, ac gravida massa blandit eget. Donec scelerisque convallis libero nec semper. Aenean ullamcorper mauris vel semper tempus. Nunc eros metus, ornare quis bibendum placerat, auctor ut leo. Nulla pharetra tempus diam, quis mattis ipsum faucibus nec. Duis efficitur sem eget magna efficitur, in dapibus est maximus. Integer porta eros sed justo luctus, quis rhoncus neque malesuada.
Cras tincidunt aliquam purus, in ornare leo varius in. Aliquam vitae feugiat elit, ut dignissim massa. Maecenas tempus scelerisque sem ac iaculis. Etiam pulvinar lectus in dictum tempus. Aenean maximus, urna non lobortis eleifend, odio odio congue nisi, sit amet ornare elit orci id enim. Phasellus quis ipsum tortor. Sed euismod velit quam, vitae pellentesque enim pulvinar quis. In tortor neque, euismod vel rhoncus nec, laoreet eu nunc.`;

  ngOnInit(): void {
  }

}
